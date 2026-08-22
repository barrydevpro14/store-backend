package org.store.security.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.BadArgumentException;
import org.store.notification.application.event.PasswordResetRequestedEvent;
import org.store.notification.application.service.IEmailEventPublisher;
import org.store.property.AppProperties;
import org.store.property.PasswordResetProperties;
import org.store.security.application.dto.ForgotPasswordRequest;
import org.store.security.application.dto.ResetPasswordConfirmRequest;
import org.store.security.application.service.impl.PasswordResetServiceImpl;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.PasswordResetToken;
import org.store.users.domain.model.Utilisateur;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private IAccountService accountService;

    @Mock
    private IPasswordResetTokenService passwordResetTokenService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private PasswordResetProperties passwordResetProperties;

    @Mock
    private IEmailEventPublisher emailEventPublisher;

    @InjectMocks
    private PasswordResetServiceImpl service;

    // --- requestReset ---

    @Test
    void requestReset_should_return_silently_when_account_not_found() {
        when(accountService.findByUsernameOrEmail("ghost")).thenReturn(Optional.empty());

        service.requestReset(new ForgotPasswordRequest("ghost"));

        verify(passwordResetTokenService, never()).deleteByAccountId(any());
        verify(emailEventPublisher, never()).publishPasswordResetRequested(any());
    }

    @Test
    void requestReset_should_return_silently_when_account_has_no_email() {
        Account account = accountWithoutEmail("john.doe");
        when(accountService.findByUsernameOrEmail("john.doe")).thenReturn(Optional.of(account));

        service.requestReset(new ForgotPasswordRequest("john.doe"));

        verify(passwordResetTokenService, never()).deleteByAccountId(any());
        verify(emailEventPublisher, never()).publishPasswordResetRequested(any());
    }

    @Test
    void requestReset_should_delete_old_tokens_save_new_and_publish_event() {
        UUID accountId = UUID.randomUUID();
        Account account = accountWithEmail(accountId, "john.doe", "John", "Doe", "john@example.com");
        PasswordResetToken savedToken = new PasswordResetToken();
        savedToken.setToken("tok-abc");

        when(accountService.findByUsernameOrEmail("john.doe")).thenReturn(Optional.of(account));
        when(passwordResetProperties.expiryHours()).thenReturn(1);
        when(appProperties.url()).thenReturn("http://localhost:3000");
        when(passwordResetTokenService.save(any())).thenReturn(savedToken);

        service.requestReset(new ForgotPasswordRequest("john.doe"));

        verify(passwordResetTokenService).deleteByAccountId(accountId);
        verify(passwordResetTokenService).save(any(PasswordResetToken.class));

        ArgumentCaptor<PasswordResetRequestedEvent> eventCaptor = ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
        verify(emailEventPublisher).publishPasswordResetRequested(eventCaptor.capture());

        PasswordResetRequestedEvent event = eventCaptor.getValue();
        assertThat(event.toEmail()).isEqualTo("john@example.com");
        assertThat(event.recipientName()).isEqualTo("John Doe");
        assertThat(event.resetLink()).startsWith("http://localhost:3000/reset-password?token=");
    }

    // --- confirmReset ---

    @Test
    void confirmReset_should_throw_when_token_not_found() {
        when(passwordResetTokenService.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmReset(new ResetPasswordConfirmRequest("invalid", "newPass1!")))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void confirmReset_should_throw_when_token_already_used() {
        PasswordResetToken usedToken = tokenWithState(true, LocalDateTime.now().plusHours(1));
        when(passwordResetTokenService.findByToken("tok")).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> service.confirmReset(new ResetPasswordConfirmRequest("tok", "newPass1!")))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void confirmReset_should_throw_when_token_expired() {
        PasswordResetToken expiredToken = tokenWithState(false, LocalDateTime.now().minusHours(1));
        when(passwordResetTokenService.findByToken("tok")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> service.confirmReset(new ResetPasswordConfirmRequest("tok", "newPass1!")))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void confirmReset_should_reset_password_and_mark_token_used() {
        Account account = new Account();
        PasswordResetToken validToken = tokenWithState(false, LocalDateTime.now().plusHours(1));
        validToken.setAccount(account);

        when(passwordResetTokenService.findByToken("tok")).thenReturn(Optional.of(validToken));

        service.confirmReset(new ResetPasswordConfirmRequest("tok", "newPass1!"));

        verify(accountService).resetPassword(account, "newPass1!");
        verify(passwordResetTokenService).save(validToken);
        assertThat(validToken.isUsed()).isTrue();
    }

    // --- resolveEmail ---

    @Test
    void resolveEmail_should_return_email_when_user_has_email() {
        Utilisateur user = new Utilisateur();
        user.setEmail("john@example.com");
        Account account = new Account();
        account.setUser(user);

        assertThat(service.resolveEmail(account)).isEqualTo("john@example.com");
    }

    @Test
    void resolveEmail_should_return_null_when_user_is_null() {
        Account account = new Account();

        assertThat(service.resolveEmail(account)).isNull();
    }

    @Test
    void resolveEmail_should_return_null_when_email_is_null() {
        Utilisateur user = new Utilisateur();
        Account account = new Account();
        account.setUser(user);

        assertThat(service.resolveEmail(account)).isNull();
    }

    // --- resolveRecipientName ---

    @Test
    void resolveRecipientName_should_return_prenom_nom_when_both_present() {
        Utilisateur user = new Utilisateur();
        user.setNom("Doe");
        user.setPrenom("John");
        Account account = new Account();
        account.setUser(user);

        assertThat(service.resolveRecipientName(account)).isEqualTo("John Doe");
    }

    @Test
    void resolveRecipientName_should_return_nom_when_prenom_is_null() {
        Utilisateur user = new Utilisateur();
        user.setNom("Doe");
        Account account = new Account();
        account.setUser(user);

        assertThat(service.resolveRecipientName(account)).isEqualTo("Doe");
    }

    @Test
    void resolveRecipientName_should_return_username_when_user_is_null() {
        Account account = new Account();
        account.setUsername("john.doe");

        assertThat(service.resolveRecipientName(account)).isEqualTo("john.doe");
    }

    // --- factories ---

    private Account accountWithEmail(UUID id, String username, String prenom, String nom, String email) {
        Utilisateur user = new Utilisateur();
        user.setPrenom(prenom);
        user.setNom(nom);
        user.setEmail(email);

        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setUser(user);
        return account;
    }

    private Account accountWithoutEmail(String username) {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername(username);
        return account;
    }

    private PasswordResetToken tokenWithState(boolean used, LocalDateTime expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("tok");
        token.setUsed(used);
        token.setExpiresAt(expiresAt);
        return token;
    }
}
