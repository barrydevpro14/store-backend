package org.store.produit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.produit.application.dto.QualityFilter;
import org.store.produit.application.dto.QualityResponse;
import org.store.produit.domain.model.Quality;

import java.util.Optional;
import java.util.UUID;

public interface QualityRepository extends BaseRepository<Quality> {

    @Query(value = """
            SELECT new org.store.produit.application.dto.QualityResponse(quality)
            FROM Quality quality
            WHERE quality.entreprise.id = :entrepriseId
              AND (
                  :#{#filter.libelle} IS NULL
                  OR :#{#filter.libelle} = ''
                  OR LOWER(quality.libelle) LIKE LOWER(CONCAT('%', :#{#filter.libelle}, '%'))
              )
              AND (:#{#filter.createdStartDateTime()} IS NULL OR quality.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR quality.createdAt <  :#{#filter.createdEndDateTime()})
            ORDER BY quality.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(quality)
            FROM Quality quality
            WHERE quality.entreprise.id = :entrepriseId
              AND (
                  :#{#filter.libelle} IS NULL
                  OR :#{#filter.libelle} = ''
                  OR LOWER(quality.libelle) LIKE LOWER(CONCAT('%', :#{#filter.libelle}, '%'))
              )
              AND (:#{#filter.createdStartDateTime()} IS NULL OR quality.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR quality.createdAt <  :#{#filter.createdEndDateTime()})
            """)
    Page<QualityResponse> findResponsesByFilter(@Param("filter") QualityFilter filter,
                                                @Param("entrepriseId") UUID entrepriseId,
                                                Pageable pageable);

    Optional<Quality> findByLibelleAndEntrepriseId(String libelle, UUID entrepriseId);

    boolean existsByLibelleAndEntrepriseId(String libelle, UUID entrepriseId);
}
