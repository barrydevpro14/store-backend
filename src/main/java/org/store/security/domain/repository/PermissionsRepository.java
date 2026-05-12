package org.store.security.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.Permissions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionsRepository extends BaseRepository<Permissions> {

    Optional<Permissions> findByCode(String code);

    @Query("""
            SELECT p.code FROM Role r
            JOIN r.permissions p
            WHERE r.id = :roleId
            """)
    List<String> findAllByRoleId(@Param("roleId") UUID roleId);
}
