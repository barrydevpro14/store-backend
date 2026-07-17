package org.store.catalogue.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.catalogue.application.dto.CatalogueProduitSummaryResponse;
import org.store.catalogue.domain.model.CatalogueProduit;
import org.store.common.repository.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface CatalogueProduitRepository extends BaseRepository<CatalogueProduit> {

    boolean existsByReferenceAndLibelleAndActiviteEconomiqueId(String reference, String libelle, UUID activiteEconomiqueId);

    @Query("""
            SELECT new org.store.catalogue.application.dto.CatalogueProduitSummaryResponse(
                c.id, c.reference, c.libelle, c.categorie, c.description
            )
            FROM CatalogueProduit c
            WHERE c.activiteEconomique.id = :activiteEconomiqueId
            ORDER BY c.libelle ASC
            """)
    List<CatalogueProduitSummaryResponse> findSummariesByActiviteEconomiqueId(@Param("activiteEconomiqueId") UUID activiteEconomiqueId);

    @Query(value = """
            SELECT new org.store.catalogue.application.dto.CatalogueProduitSummaryResponse(
                c.id, c.reference, c.libelle, c.categorie, c.description
            )
            FROM CatalogueProduit c
            WHERE c.activiteEconomique.id = :activiteEconomiqueId
              AND (:reference IS NULL OR LOWER(c.reference) LIKE :referencePattern)
              AND (:libelle   IS NULL OR LOWER(c.libelle)   LIKE :libellePattern)
              AND (:categorie IS NULL OR LOWER(c.categorie) LIKE :categoriePattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', c.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', c.createdAt) <= CAST(:endDate   AS date))
            ORDER BY c.libelle ASC
            """,
           countQuery = """
            SELECT COUNT(c)
            FROM CatalogueProduit c
            WHERE c.activiteEconomique.id = :activiteEconomiqueId
              AND (:reference IS NULL OR LOWER(c.reference) LIKE :referencePattern)
              AND (:libelle   IS NULL OR LOWER(c.libelle)   LIKE :libellePattern)
              AND (:categorie IS NULL OR LOWER(c.categorie) LIKE :categoriePattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', c.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', c.createdAt) <= CAST(:endDate   AS date))
            """)
    Page<CatalogueProduitSummaryResponse> findByFilter(
            @Param("activiteEconomiqueId") UUID activiteEconomiqueId,
            @Param("reference") String reference,
            @Param("referencePattern") String referencePattern,
            @Param("libelle") String libelle,
            @Param("libellePattern") String libellePattern,
            @Param("categorie") String categorie,
            @Param("categoriePattern") String categoriePattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable
    );
}
