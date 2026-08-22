package org.store.security.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends BaseRepository<PasswordResetToken> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByAccount_Id(UUID accountId);
}
