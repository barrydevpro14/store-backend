package org.store.depense.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.depense.application.dto.CategoryDepenseResponse;
import org.store.depense.domain.model.CategoryDepense;

import java.util.Optional;
import java.util.UUID;

public interface CategoryDepenseRepository extends BaseRepository<CategoryDepense> {

    Optional<CategoryDepense> findByNomAndEntrepriseId(String nom, UUID entrepriseId);

    boolean existsByNomAndEntrepriseId(String nom, UUID entrepriseId);

    @Query("""
            SELECT new org.store.depense.application.dto.CategoryDepenseResponse(category)
            FROM CategoryDepense category
            WHERE category.entreprise.id = :entrepriseId
            ORDER BY category.nom ASC
            """)
    Page<CategoryDepenseResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId, Pageable pageable);
}
