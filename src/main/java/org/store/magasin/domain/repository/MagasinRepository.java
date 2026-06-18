package org.store.magasin.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.magasin.domain.model.Magasin;

import java.util.List;
import java.util.UUID;

public interface MagasinRepository extends BaseRepository<Magasin> {

    @Query("SELECT magasin.entreprise.id, COUNT(magasin) FROM Magasin magasin GROUP BY magasin.entreprise.id")
    List<Object[]> countAllGroupByEntrepriseId();

    @Query("SELECT COUNT(magasin) FROM Magasin magasin WHERE magasin.actif = :actif")
    long countByActif(@Param("actif") boolean actif);

    @Query("SELECT COUNT(magasin) FROM Magasin magasin WHERE magasin.entreprise.id = :entrepriseId")
    long countByEntrepriseId(@Param("entrepriseId") UUID entrepriseId);

    @Query(value = """
            SELECT new org.store.magasin.application.dto.MagasinResponse(magasin)
            FROM Magasin magasin
            WHERE magasin.entreprise.id = :entrepriseId
              AND (:nom IS NULL OR :nom = '' OR LOWER(magasin.nom) LIKE :nomPattern)
              AND (:actif IS NULL OR magasin.actif = :actif)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', magasin.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', magasin.createdAt) <= CAST(:endDate AS date))
            ORDER BY magasin.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(magasin)
            FROM Magasin magasin
            WHERE magasin.entreprise.id = :entrepriseId
              AND (:nom IS NULL OR :nom = '' OR LOWER(magasin.nom) LIKE :nomPattern)
              AND (:actif IS NULL OR magasin.actif = :actif)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', magasin.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', magasin.createdAt) <= CAST(:endDate AS date))
            """)
    Page<MagasinResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("nom") String nom,
            @Param("nomPattern") String nomPattern,
            @Param("actif") Boolean actif,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

        @Query(value = """
                    SELECT new org.store.magasin.application.dto.MagasinSummaryResponse(magasin)
                    FROM Magasin magasin
                    WHERE magasin.entreprise.id = :entrepriseId
                    AND magasin.actif = :actif
                    ORDER BY magasin.createdAt DESC
                    """)
        List<MagasinSummaryResponse> findAllByEntreprise(
                @Param("entrepriseId") UUID entrepriseId,
                @Param("actif") boolean actif
               );
}
