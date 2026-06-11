package org.store.security.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.notification.application.event.PasswordResetRequestedEvent;
import org.store.notification.application.service.IEmailEventPublisher;
import org.store.property.AppProperties;
import org.store.security.application.dto.ForgotPasswordRequest;
import org.store.security.application.dto.ResetPasswordConfirmRequest;
import org.store.security.application.service.IAccountService;
import org.store.security.application.service.IPasswordResetService;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.PasswordResetToken;
import org.store.security.domain.repository.AccountRepository;
import org.store.security.domain.repository.PasswordResetTokenRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Gère le cycle « mot de passe oublié » :
 * <ol>
 *   <li>requestReset  — cherche le compte par username ou e-mail, génère un token UUID,
 *       supprime les anciens tokens du compte, envoie le lien par e-mail.</li>
 *   <li>confirmReset  — valide le token (non expiré, non consommé), réinitialise
 *       le mot de passe via AccountService, marque le token consommé.</li>
 * </ol>
 * Les deux méthodes ne révèlent pas l'existence d'un compte (HTTP 204 dans tous les cas).
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetServiceImpl implements IPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
    private static final int TOKEN_EXPIRY_HOURS = 1;

    private final AccountRepository accountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final IAccountService accountService;
    private final AppProperties appProperties;
    private final IEmailEventPublisher emailEventPublisher;

    public PasswordResetServiceImpl(AccountRepository accountRepository,
                                    PasswordResetTokenRepository tokenRepository,
                                    IAccountService accountService,
                                    AppProperties appProperties,
                                    IEmailEventPublisher emailEventPublisher) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.accountService = accountService;
        this.appProperties = appProperties;
        this.emailEventPublisher = emailEventPublisher;
    }

    @Override
    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        Optional<Account> accountOpt = accountRepository.findByUsernameOrEmail(request.identifier().trim());

        if (accountOpt.isEmpty()) {
            log.debug("Password reset requested for unknown identifier: {}", request.identifier());
            return;
        }

        Account account = accountOpt.get();
        String toEmail = resolveEmail(account);

        if (toEmail == null) {
            log.warn("No email found for account {}", account.getId());
            return;
        }

        tokenRepository.deleteByAccount_Id(account.getId());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setAccount(account);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
        tokenRepository.save(resetToken);

        String resetLink = appProperties.url() + "/reset-password?token=" + resetToken.getToken();
        String recipientName = resolveRecipientName(account);

        emailEventPublisher.publishPasswordResetRequested(new PasswordResetRequestedEvent(toEmail, recipientName, resetLink));
        log.info("PasswordResetRequested event published for account {}", account.getId());
    }

    @Override
    @Transactional
    public void confirmReset(ResetPasswordConfirmRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BadArgumentException("passwordReset.invalidToken"));

        if (resetToken.isUsed()) {
            throw new BadArgumentException("passwordReset.tokenAlreadyUsed");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadArgumentException("passwordReset.tokenExpired");
        }

        accountService.resetPassword(resetToken.getAccount(), request.newPassword());

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private String resolveEmail(Account account) {
        if (account.getUser() != null && account.getUser().getEmail() != null) {
            return account.getUser().getEmail();
        }
        return null;
    }

    private String resolveRecipientName(Account account) {
        if (account.getUser() != null) {
            String nom = account.getUser().getNom();
            String prenom = account.getUser().getPrenom();
            if (nom != null && prenom != null) return prenom + " " + nom;
            if (nom != null) return nom;
        }
        return account.getUsername();
    }
}
