package org.store.achat.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.achat.application.dto.FournisseurFilter;
import org.store.achat.application.dto.FournisseurResponse;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.repository.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface FournisseurRepository extends BaseRepository<Fournisseur> {

    /**
     * Listing entreprise-scope avec filtres optionnels sur `nom`,
     * `reference` et fenêtre de création. ORDER BY createdAt DESC pour
     * que les nouveaux fournisseurs apparaissent en haut de page 1.
     */
    @Query(value = """
            SELECT new org.store.achat.application.dto.FournisseurResponse(fournisseur)
            FROM Fournisseur fournisseur
            WHERE fournisseur.entreprise.id = :entrepriseId
              AND (
                  :#{#filter.nom} IS NULL
                  OR :#{#filter.nom} = ''
                  OR LOWER(fournisseur.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%'))
              )
              AND (
                  :#{#filter.reference} IS NULL
                  OR :#{#filter.reference} = ''
                  OR LOWER(fournisseur.reference) LIKE LOWER(CONCAT('%', :#{#filter.reference}, '%'))
              )
              AND (:#{#filter.createdStartDateTime()} IS NULL OR fournisseur.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR fournisseur.createdAt <  :#{#filter.createdEndDateTime()})
            ORDER BY fournisseur.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(fournisseur)
            FROM Fournisseur fournisseur
            WHERE fournisseur.entreprise.id = :entrepriseId
              AND (
                  :#{#filter.nom} IS NULL
                  OR :#{#filter.nom} = ''
                  OR LOWER(fournisseur.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%'))
              )
              AND (
                  :#{#filter.reference} IS NULL
                  OR :#{#filter.reference} = ''
                  OR LOWER(fournisseur.reference) LIKE LOWER(CONCAT('%', :#{#filter.reference}, '%'))
              )
              AND (:#{#filter.createdStartDateTime()} IS NULL OR fournisseur.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR fournisseur.createdAt <  :#{#filter.createdEndDateTime()})
            """)
    Page<FournisseurResponse> findResponsesByFilter(@Param("filter") FournisseurFilter filter,
                                                   @Param("entrepriseId") UUID entrepriseId,
                                                   Pageable pageable);

    Optional<Fournisseur> findByReferenceAndEntrepriseId(String reference, UUID entrepriseId);

    boolean existsByReferenceAndEntrepriseId(String reference, UUID entrepriseId);
}
