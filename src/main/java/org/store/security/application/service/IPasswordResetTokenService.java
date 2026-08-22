package org.store.security.application.service;

import org.store.security.domain.model.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface IPasswordResetTokenService {

    /** Finds a token by its raw value. */
    Optional<PasswordResetToken> findByToken(String token);

    /** Deletes all tokens belonging to the given account. */
    void deleteByAccountId(UUID accountId);

    /** Persists a token. */
    PasswordResetToken save(PasswordResetToken passwordResetToken);
}
