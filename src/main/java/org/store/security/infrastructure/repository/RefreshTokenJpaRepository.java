package org.store.security.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.security.domain.model.RefreshToken;
import org.store.security.domain.repository.RefreshTokenRepository;

import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, UUID>, RefreshTokenRepository {
}
