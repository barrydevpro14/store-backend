package org.store.security.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.security.domain.model.Role;

import java.util.UUID;

public interface RoleJpaRepository extends JpaRepository<Role, UUID> {
}
