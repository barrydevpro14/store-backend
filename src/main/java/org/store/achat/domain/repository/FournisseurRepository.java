package org.store.achat.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.achat.application.dto.FournisseurResponse;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.repository.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface FournisseurRepository extends BaseRepository<Fournisseur> {

    /**
     * Listing scoped to a company + global system suppliers (entreprise IS NULL).
     * System suppliers appear first (systeme DESC), then by createdAt DESC.
     */
    @Query(value = """
            SELECT new org.store.achat.application.dto.FournisseurResponse(fournisseur)
            FROM Fournisseur fournisseur
            WHERE (fournisseur.entreprise.id = :entrepriseId OR fournisseur.entreprise IS NULL)
              AND (:nom IS NULL OR :nom = '' OR LOWER(fournisseur.nom) LIKE :nomPattern)
              AND (:reference IS NULL OR :reference = '' OR LOWER(fournisseur.reference) LIKE :referencePattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', fournisseur.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', fournisseur.createdAt) <= CAST(:endDate AS date))
            ORDER BY fournisseur.systeme DESC, fournisseur.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(fournisseur)
            FROM Fournisseur fournisseur
            WHERE (fournisseur.entreprise.id = :entrepriseId OR fournisseur.entreprise IS NULL)
              AND (:nom IS NULL OR :nom = '' OR LOWER(fournisseur.nom) LIKE :nomPattern)
              AND (:reference IS NULL OR :reference = '' OR LOWER(fournisseur.reference) LIKE :referencePattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', fournisseur.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', fournisseur.createdAt) <= CAST(:endDate AS date))
            """)
    Page<FournisseurResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("nom") String nom,
            @Param("nomPattern") String nomPattern,
            @Param("reference") String reference,
            @Param("referencePattern") String referencePattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    /** Finds a fournisseur by reference scoped to a company. */
    Optional<Fournisseur> findByReferenceAndEntrepriseId(String reference, UUID entrepriseId);

    boolean existsByReferenceAndEntrepriseId(String reference, UUID entrepriseId);

    /** Finds the unique global system supplier by reference (entreprise = null). */
    @Query("SELECT f FROM Fournisseur f WHERE f.reference = :reference AND f.entreprise IS NULL")
    Optional<Fournisseur> findGlobalByReference(@Param("reference") String reference);
}
