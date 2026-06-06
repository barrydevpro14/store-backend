package org.store.security.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends BaseRepository<Role> {

    /** Finds a global (system) role by name — entreprise must be null. */
    @Query("SELECT r FROM Role r WHERE r.libelle = :libelle AND r.entreprise IS NULL")
    Optional<Role> findByLibelle(@Param("libelle") String libelle);

    /**
     * Roles visible to a given company:
     * — company-scoped custom roles
     * — global roles (entreprise = null) that have no company-scoped override with the same name (case-insensitive)
     */
    @Query("""
            SELECT r FROM Role r LEFT JOIN FETCH r.permissions
            WHERE r.entreprise.id = :entrepriseId
               OR (r.entreprise IS NULL
                   AND NOT EXISTS (
                       SELECT 1 FROM Role cr
                       WHERE cr.entreprise.id = :entrepriseId
                         AND LOWER(cr.libelle) = LOWER(r.libelle)
                   ))
            """)
    List<Role> findByEntrepriseIdOrGlobal(@Param("entrepriseId") UUID entrepriseId);

    /** All roles — for ADMIN only. */
    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions")
    List<Role> findAllWithPermissions();

    /** Check if any account is assigned to this role (prevents deletion). */
    @Query("SELECT COUNT(a) > 0 FROM Account a WHERE a.role = :role")
    boolean existsUserWithRole(@Param("role") Role role);

    /** Case-insensitive uniqueness check: libelle + entreprise scope (excludes self on update). */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE LOWER(r.libelle) = LOWER(:libelle) AND r.entreprise.id = :entrepriseId AND r.id <> :excludeId")
    boolean existsByLibelleAndEntrepriseExcluding(@Param("libelle") String libelle,
                                                  @Param("entrepriseId") UUID entrepriseId,
                                                  @Param("excludeId") UUID excludeId);

    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE LOWER(r.libelle) = LOWER(:libelle) AND r.entreprise.id = :entrepriseId")
    boolean existsByLibelleAndEntreprise(@Param("libelle") String libelle,
                                         @Param("entrepriseId") UUID entrepriseId);
}
