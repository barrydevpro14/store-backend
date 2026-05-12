package org.store.security.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.RoleRepository;

import java.util.UUID;

@Repository
public interface RoleJpaRepository extends JpaRepository<Role, UUID>, RoleRepository {
}
