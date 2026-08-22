package org.store.security.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.notification.application.event.PasswordResetRequestedEvent;
import org.store.notification.application.service.IEmailEventPublisher;
import org.store.property.AppProperties;
import org.store.property.PasswordResetProperties;
import org.store.security.application.dto.ForgotPasswordRequest;
import org.store.security.application.dto.ResetPasswordConfirmRequest;
import org.store.security.application.service.IAccountService;
import org.store.security.application.service.IPasswordResetService;
import org.store.security.application.service.IPasswordResetTokenService;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.PasswordResetToken;
import org.store.users.domain.model.Utilisateur;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the forgot-password cycle: token generation on request, validation and password reset on confirmation.
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetServiceImpl implements IPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

    private final IAccountService accountService;
    private final IPasswordResetTokenService passwordResetTokenService;
    private final AppProperties appProperties;
    private final PasswordResetProperties passwordResetProperties;
    private final IEmailEventPublisher emailEventPublisher;

    public PasswordResetServiceImpl(IAccountService accountService,
                                    IPasswordResetTokenService passwordResetTokenService,
                                    AppProperties appProperties,
                                    PasswordResetProperties passwordResetProperties,
                                    IEmailEventPublisher emailEventPublisher) {
        this.accountService = accountService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.appProperties = appProperties;
        this.passwordResetProperties = passwordResetProperties;
        this.emailEventPublisher = emailEventPublisher;
    }

    @Override
    @Transactional
    public void requestReset(ForgotPasswordRequest forgotPasswordRequest) {
        Optional<Account> foundAccount = accountService.findByUsernameOrEmail(forgotPasswordRequest.identifier().trim());

        if (foundAccount.isEmpty()) {
            log.debug("Password reset requested for unknown identifier: {}", forgotPasswordRequest.identifier());
            return;
        }

        Account account = foundAccount.get();
        UUID accountId = account.getId();
        String recipientEmail = resolveEmail(account);

        if (recipientEmail == null) {
            log.warn("No email found for account {}", accountId);
            return;
        }

        passwordResetTokenService.deleteByAccountId(accountId);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setAccount(account);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(passwordResetProperties.expiryHours()));
        passwordResetTokenService.save(resetToken);

        String resetLink = appProperties.url() + "/reset-password?token=" + resetToken.getToken();
        String recipientName = resolveRecipientName(account);

        emailEventPublisher.publishPasswordResetRequested(new PasswordResetRequestedEvent(recipientEmail, recipientName, resetLink));
        log.info("PasswordResetRequested event published for account {}", accountId);
    }

    @Override
    @Transactional
    public void confirmReset(ResetPasswordConfirmRequest resetPasswordConfirmRequest) {
        PasswordResetToken resetToken = passwordResetTokenService.findByToken(resetPasswordConfirmRequest.token())
                .orElseThrow(() -> new BadArgumentException("passwordReset.invalidToken"));

        if (resetToken.isUsed()) {
            throw new BadArgumentException("passwordReset.tokenAlreadyUsed");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadArgumentException("passwordReset.tokenExpired");
        }

        accountService.resetPassword(resetToken.getAccount(), resetPasswordConfirmRequest.newPassword());

        resetToken.setUsed(true);
        passwordResetTokenService.save(resetToken);
    }

    @Override
    public String resolveEmail(Account account) {
        if (account.getUser() != null && account.getUser().getEmail() != null) {
            return account.getUser().getEmail();
        }
        return null;
    }

    @Override
    public String resolveRecipientName(Account account) {
        Utilisateur user = account.getUser();

        if (user != null) {
            String nom = user.getNom();
            String prenom = user.getPrenom();
            if (nom != null && prenom != null) return prenom + " " + nom;
            if (nom != null) return nom;
        }

        return account.getUsername();
    }
}
