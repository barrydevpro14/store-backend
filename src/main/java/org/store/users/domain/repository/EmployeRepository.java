package org.store.users.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.Account;
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
              AND (:nom IS NULL OR :nom = '' OR LOWER(employe.nom) LIKE :nomPattern)
              AND (:prenom IS NULL OR :prenom = '' OR LOWER(employe.prenom) LIKE :prenomPattern)
              AND (:role IS NULL OR :role = '' OR employe.account.role.libelle = :role)
              AND (:magasinId IS NULL OR employe.magasin.id = :magasinId)
              AND (:actif IS NULL OR employe.account.enabled = :actif)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', employe.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', employe.createdAt) <= CAST(:endDate AS date))
            ORDER BY employe.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(employe)
            FROM Employe employe
            WHERE employe.magasin.entreprise.id = :entrepriseId
              AND (:nom IS NULL OR :nom = '' OR LOWER(employe.nom) LIKE :nomPattern)
              AND (:prenom IS NULL OR :prenom = '' OR LOWER(employe.prenom) LIKE :prenomPattern)
              AND (:role IS NULL OR :role = '' OR employe.account.role.libelle = :role)
              AND (:magasinId IS NULL OR employe.magasin.id = :magasinId)
              AND (:actif IS NULL OR employe.account.enabled = :actif)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', employe.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', employe.createdAt) <= CAST(:endDate AS date))
            """)
    Page<EmployeResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("nom") String nom,
            @Param("nomPattern") String nomPattern,
            @Param("prenom") String prenom,
            @Param("prenomPattern") String prenomPattern,
            @Param("role") String role,
            @Param("magasinId") UUID magasinId,
            @Param("actif") Boolean actif,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
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
