package org.store.produit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.produit.application.dto.QualityResponse;
import org.store.produit.application.dto.QualitySummaryResponse;
import org.store.produit.domain.model.Quality;

import java.util.Optional;
import java.util.UUID;

public interface QualityRepository extends BaseRepository<Quality> {

    @Query(value = """
            SELECT new org.store.produit.application.dto.QualityResponse(quality)
            FROM Quality quality
            WHERE quality.entreprise.id = :entrepriseId
              AND (:libelle IS NULL OR :libelle = '' OR LOWER(CONCAT(quality.libelle,quality.description)) LIKE :libellePattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', quality.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', quality.createdAt) <= CAST(:endDate AS date))
            ORDER BY quality.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(quality)
            FROM Quality quality
            WHERE quality.entreprise.id = :entrepriseId
              AND (:libelle IS NULL OR :libelle = '' OR LOWER(CONCAT(quality.libelle,quality.description)) LIKE :libellePattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', quality.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', quality.createdAt) <= CAST(:endDate AS date))
            """)
    Page<QualityResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("libelle") String libelle,
            @Param("libellePattern") String libellePattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query(value = """
            SELECT new org.store.produit.application.dto.QualitySummaryResponse(quality)
            FROM Quality quality
            WHERE quality.entreprise.id = :entrepriseId
              AND (:searchPattern IS NULL OR LOWER(quality.libelle) LIKE :searchPattern)
            ORDER BY quality.libelle ASC
            """,
           countQuery = """
            SELECT COUNT(quality)
            FROM Quality quality
            WHERE quality.entreprise.id = :entrepriseId
              AND (:searchPattern IS NULL OR LOWER(quality.libelle) LIKE :searchPattern)
            """)
    Page<QualitySummaryResponse> searchSummaries(@Param("entrepriseId") UUID entrepriseId,
                                                 @Param("searchPattern") String searchPattern,
                                                 Pageable pageable);

    Optional<Quality> findByLibelleAndEntrepriseId(String libelle, UUID entrepriseId);

    @Query("SELECT COUNT(p) > 0 FROM Quality p WHERE LOWER(p.libelle) = LOWER(:libelle) AND p.entreprise.id = :entrepriseId")
    boolean existsByLibelleAndEntrepriseId(@Param("libelle") String libelle, @Param("entrepriseId") UUID entrepriseId);
}
