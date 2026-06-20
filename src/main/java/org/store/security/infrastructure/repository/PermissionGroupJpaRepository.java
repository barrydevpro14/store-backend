package org.store.security.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.store.security.domain.model.PermissionGroup;
import org.store.security.domain.repository.PermissionGroupRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PermissionGroupJpaRepository extends JpaRepository<PermissionGroup, UUID>, PermissionGroupRepository {

    @Override
    @Query("SELECT DISTINCT pg FROM PermissionGroup pg LEFT JOIN FETCH pg.permissions ORDER BY pg.libelle ASC")
    List<PermissionGroup> findAllWithPermissions();

    @Override
    @Query("""
            SELECT DISTINCT pg FROM PermissionGroup pg
            JOIN FETCH pg.permissions p
            WHERE p.assignableToCustomRole = true
            ORDER BY pg.libelle ASC
            """)
    List<PermissionGroup> findGroupsWithAssignablePermissions();
}
