package org.store.users.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.Account;
import org.store.users.application.dto.EmployeFilter;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.domain.model.Employe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeRepository extends BaseRepository<Employe> {

    @Query("SELECT employe.magasin.entreprise.id, COUNT(employe) FROM Employe employe GROUP BY employe.magasin.entreprise.id")
    List<Object[]> countAllGroupByEntrepriseId();

    @Query("SELECT COUNT(employe) FROM Employe employe WHERE employe.magasin.entreprise.id = :entrepriseId")
    long countByEntrepriseId(@Param("entrepriseId") UUID entrepriseId);

    @Query(value = """
            SELECT new org.store.users.application.dto.EmployeResponse(employe)
            FROM Employe employe
            WHERE employe.magasin.entreprise.id = :entrepriseId
              AND (:#{#filter.nom} IS NULL OR LOWER(employe.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.prenom} IS NULL OR LOWER(employe.prenom) LIKE LOWER(CONCAT('%', :#{#filter.prenom}, '%')))
              AND (:#{#filter.role} IS NULL OR employe.account.role.libelle = :#{#filter.role})
              AND (:#{#filter.magasinId} IS NULL OR employe.magasin.id = :#{#filter.magasinId})
              AND (:#{#filter.actif} IS NULL OR employe.account.enabled = :#{#filter.actif})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR employe.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR employe.createdAt <  :#{#filter.createdEndDateTime()})
            ORDER BY employe.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(employe)
            FROM Employe employe
            WHERE employe.magasin.entreprise.id = :entrepriseId
              AND (:#{#filter.nom} IS NULL OR LOWER(employe.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.prenom} IS NULL OR LOWER(employe.prenom) LIKE LOWER(CONCAT('%', :#{#filter.prenom}, '%')))
              AND (:#{#filter.role} IS NULL OR employe.account.role.libelle = :#{#filter.role})
              AND (:#{#filter.magasinId} IS NULL OR employe.magasin.id = :#{#filter.magasinId})
              AND (:#{#filter.actif} IS NULL OR employe.account.enabled = :#{#filter.actif})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR employe.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR employe.createdAt <  :#{#filter.createdEndDateTime()})
            """)
    Page<EmployeResponse> findResponsesByFilter(@Param("filter") EmployeFilter filter,
                                                @Param("entrepriseId") UUID entrepriseId,
                                                Pageable pageable);

    @Query("SELECT COUNT(e) FROM Employe e WHERE e.magasin.id = :magasinId")
    long countByMagasinId(@Param("magasinId") UUID magasinId);

    @Query("SELECT e.account FROM Employe e WHERE e.magasin.id = :magasinId AND e.account.role.libelle = :roleLibelle AND e.account.enabled = true")
    List<Account> findActiveAccountsByMagasinIdAndRoleLibelle(@Param("magasinId") UUID magasinId,
                                                              @Param("roleLibelle") String roleLibelle);

    @Query("""
            SELECT new org.store.users.application.dto.EmployeResponse(employe)
            FROM Employe employe
            WHERE employe.id = :id
              AND employe.magasin.entreprise.id = :entrepriseId
            """)
    Optional<EmployeResponse> findResponseById(@Param("id") UUID id,
                                               @Param("entrepriseId") UUID entrepriseId);
}
