package org.store.security.application.service;

import org.store.security.application.service.impl.RefreshTokenServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.UnauthorisedException;
import org.store.property.JwtProperties;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.RefreshToken;
import org.store.security.domain.service.RefreshTokenDomainService;
import org.store.users.domain.model.Proprietaire;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.service.AuditLogDomainService;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenDomainService refreshTokenDomainService;

    @Mock
    private IJwtService jwtService;

    @Mock
    private IUserPrincipalFactory userPrincipalFactory;

    private JwtProperties jwtProperties;
    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "Authorization",
                "Bearer ",
                new JwtProperties.Expiration(Duration.ofHours(1), Duration.ofDays(7))
        );
        service = new RefreshTokenServiceImpl(refreshTokenDomainService, jwtService, userPrincipalFactory, jwtProperties, mock(IAuditEventPublisher.class), mock(AuditLogDomainService.class), mock(IAbonnementService.class));
    }

    @Test
    void should_create_refresh_token_for_account_with_uuid_and_expiry() {
        Proprietaire user = new Proprietaire();
        Account account = new Account();
        account.setUser(user);
        when(refreshTokenDomainService.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String token = service.create(account);

        assertThat(token).isNotBlank();
        UUID.fromString(token);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenDomainService).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getToken()).isEqualTo(token);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getExpiryDate()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    void should_refresh_and_return_new_access_token_when_refresh_token_valid() {
        Account account = new Account();
        Proprietaire user = new Proprietaire();
        user.setAccount(account);
        account.setUser(user);

        RefreshToken token = new RefreshToken();
        token.setToken("rt-value");
        token.setUser(user);
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(refreshTokenDomainService.findByToken("rt-value")).thenReturn(Optional.of(token));
        when(userPrincipalFactory.build(account)).thenReturn(samplePrincipal());
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("new.access.token");

        AuthResponse response = service.refresh("rt-value");

        assertThat(response.accessToken()).isEqualTo("new.access.token");
        assertThat(response.refreshToken()).isEqualTo("rt-value");
    }

    @Test
    void should_throw_unauthorised_when_refresh_token_not_found() {
        when(refreshTokenDomainService.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("unknown"))
                .isInstanceOf(UnauthorisedException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void should_throw_unauthorised_when_refresh_token_revoked() {
        RefreshToken token = new RefreshToken();
        token.setToken("rt-value");
        token.setRevoked(true);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        when(refreshTokenDomainService.findByToken("rt-value")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.refresh("rt-value"))
                .isInstanceOf(UnauthorisedException.class);
    }

    @Test
    void should_throw_unauthorised_when_refresh_token_expired() {
        RefreshToken token = new RefreshToken();
        token.setToken("rt-value");
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().minusDays(1));
        when(refreshTokenDomainService.findByToken("rt-value")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.refresh("rt-value"))
                .isInstanceOf(UnauthorisedException.class);
    }

    @Test
    void should_revoke_existing_refresh_token() {
        RefreshToken token = new RefreshToken();
        token.setToken("rt-value");
        token.setRevoked(false);
        when(refreshTokenDomainService.findByToken("rt-value")).thenReturn(Optional.of(token));
        when(refreshTokenDomainService.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        service.revoke("rt-value");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenDomainService).save(captor.capture());
        assertThat(captor.getValue().isRevoked()).isTrue();
    }

    @Test
    void should_be_idempotent_when_revoking_already_revoked_token() {
        RefreshToken token = new RefreshToken();
        token.setToken("rt-value");
        token.setRevoked(true);
        when(refreshTokenDomainService.findByToken("rt-value")).thenReturn(Optional.of(token));

        service.revoke("rt-value");

        verify(refreshTokenDomainService, never()).save(any());
    }

    @Test
    void should_silently_succeed_when_revoking_unknown_token() {
        when(refreshTokenDomainService.findByToken("unknown")).thenReturn(Optional.empty());

        service.revoke("unknown");

        verify(refreshTokenDomainService, never()).save(any());
    }

    private UserPrincipal samplePrincipal() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), null, null, "user", null, null, "OWNER", List.of());
    }
}
