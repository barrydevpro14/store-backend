package org.store.security.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.security.domain.model.PasswordResetToken;
import org.store.security.domain.repository.PasswordResetTokenRepository;

import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetToken, UUID>, PasswordResetTokenRepository {
}
