package org.store.security.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.repository.PermissionsRepository;

import java.util.UUID;

public interface PermissionsJpaRepository extends JpaRepository<Permissions, UUID>, PermissionsRepository {
}
