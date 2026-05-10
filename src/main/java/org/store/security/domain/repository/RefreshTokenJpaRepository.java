package org.store.security.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.security.domain.model.RefreshToken;

import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, UUID> {
}
