package org.store.security.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.security.domain.model.Permissions;

import java.util.UUID;

public interface PermissionsJpaRepository extends JpaRepository<Permissions, UUID> {
}
