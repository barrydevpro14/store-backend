package org.store.security.application.service.impl;

import org.springframework.stereotype.Service;
import org.store.security.application.service.IPasswordResetTokenService;
import org.store.security.domain.model.PasswordResetToken;
import org.store.security.domain.service.PasswordResetTokenDomainService;

import java.util.Optional;
import java.util.UUID;

/**
 * Manages persistence operations on PasswordResetToken entities.
 */
@Service
public class PasswordResetTokenServiceImpl implements IPasswordResetTokenService {

    private final PasswordResetTokenDomainService passwordResetTokenDomainService;

    public PasswordResetTokenServiceImpl(PasswordResetTokenDomainService passwordResetTokenDomainService) {
        this.passwordResetTokenDomainService = passwordResetTokenDomainService;
    }

    /** Finds a token by its raw value. */
    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return passwordResetTokenDomainService.findByToken(token);
    }

    /** Deletes all tokens belonging to the given account. */
    @Override
    public void deleteByAccountId(UUID accountId) {
        passwordResetTokenDomainService.deleteByAccountId(accountId);
    }

    /** Persists a token. */
    @Override
    public PasswordResetToken save(PasswordResetToken passwordResetToken) {
        return passwordResetTokenDomainService.save(passwordResetToken);
    }
}
