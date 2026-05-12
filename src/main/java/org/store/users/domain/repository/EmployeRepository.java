package org.store.users.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.users.domain.model.Employe;

import java.util.UUID;

public interface EmployeRepository extends BaseRepository<Employe> {

    @Query("""
            SELECT COUNT(e) > 0 FROM Employe e
            JOIN e.account a JOIN a.role r JOIN r.permissions p
            WHERE e.magasin.id = :magasinId AND p.code = :permissionCode
            """)
    boolean existsByMagasinIdAndRolePermissionCode(@Param("magasinId") UUID magasinId,
                                                   @Param("permissionCode") String permissionCode);
}
