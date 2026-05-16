package org.store.produit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.produit.application.dto.QualityResponse;
import org.store.produit.domain.model.Quality;

import java.util.Optional;
import java.util.UUID;

public interface QualityRepository extends BaseRepository<Quality> {

    @Query("""
            SELECT new org.store.produit.application.dto.QualityResponse(quality)
            FROM Quality quality
            WHERE quality.entreprise.id = :entrepriseId
            """)
    Page<QualityResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId, Pageable pageable);

    Optional<Quality> findByLibelleAndEntrepriseId(String libelle, UUID entrepriseId);

    boolean existsByLibelleAndEntrepriseId(String libelle, UUID entrepriseId);
}
