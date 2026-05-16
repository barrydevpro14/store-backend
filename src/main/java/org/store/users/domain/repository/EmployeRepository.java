package org.store.users.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.users.domain.model.Employe;

import java.util.UUID;

public interface EmployeRepository extends BaseRepository<Employe> {

    @Query("""
            SELECT COUNT(employe) > 0 FROM Employe employe
            JOIN employe.account account JOIN account.role role JOIN role.permissions permission
            WHERE employe.magasin.id = :magasinId AND permission.code = :permissionCode
            """)
    boolean existsByMagasinIdAndRolePermissionCode(@Param("magasinId") UUID magasinId,
                                                   @Param("permissionCode") String permissionCode);
}
