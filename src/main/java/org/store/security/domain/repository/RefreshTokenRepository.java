package org.store.security.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends BaseRepository<RefreshToken> {

    Optional<RefreshToken> findByToken(String token);
}
