package org.store.security.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.security.application.dto.RoleListResponse;
import org.store.security.domain.model.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends BaseRepository<Role> {

    /** Finds a system role by name (case-insensitive). */
    @Query("SELECT r FROM Role r WHERE LOWER(r.libelle) = LOWER(:libelle) AND r.entreprise IS NULL")
    Optional<Role> findByLibelle(@Param("libelle") String libelle);

    /** True if a system role with the given name already exists (case-insensitive). */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE LOWER(r.libelle) = LOWER(:libelle) AND r.entreprise IS NULL")
    boolean existsByLibelleSystem(@Param("libelle") String libelle);

    /** System roles only (entreprise IS NULL) — DTO projection, no permissions. */
    @Query("""
            SELECT new org.store.security.application.dto.RoleListResponse(r)
            FROM Role r
            WHERE r.entreprise IS NULL
            ORDER BY r.libelle ASC
            """)
    List<RoleListResponse> findAllSystem();

    /** Assignable active roles for a given company + global system assignable roles — DTO projection. */
    @Query("""
            SELECT new org.store.security.application.dto.RoleListResponse(r)
            FROM Role r
            WHERE r.assignableToEmploye = true
            AND r.actif = true
            AND (r.entreprise.id = :entrepriseId OR r.entreprise IS NULL)
            ORDER BY r.libelle ASC
            """)
    List<RoleListResponse> findAssignableByEntreprise(@Param("entrepriseId") UUID entrepriseId);

    /**
     * All custom roles of a company (any status) + system assignable roles —
     * for the OWNER role management page (settings).
     */
    @Query("""
            SELECT new org.store.security.application.dto.RoleListResponse(r)
            FROM Role r
            WHERE r.entreprise.id = :entrepriseId
               OR (r.entreprise IS NULL AND r.assignableToEmploye = true)
            ORDER BY r.libelle ASC
            """)
    List<RoleListResponse> findAllByEntreprise(@Param("entrepriseId") UUID entrepriseId);

    /** Single role with its permissions eagerly loaded — entity query, mapped to DTO in the service. */
    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<Role> findByIdEager(@Param("id") UUID id);

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
