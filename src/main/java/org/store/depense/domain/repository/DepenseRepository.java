package org.store.depense.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.depense.application.dto.DepenseFilter;
import org.store.depense.application.dto.DepenseResponse;
import org.store.depense.application.dto.DepenseTotalResponse;
import org.store.depense.domain.model.Depense;

import java.util.UUID;

public interface DepenseRepository extends BaseRepository<Depense> {

    @Query("""
            SELECT new org.store.depense.application.dto.DepenseResponse(depense)
            FROM Depense depense
            WHERE depense.magasin.entreprise.id = :entrepriseId
              AND depense.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.categoryId} IS NULL OR depense.category.id = :#{#filter.categoryId})
              AND (:#{#filter.modePaiementAsEnum()} IS NULL OR depense.modePaiement = :#{#filter.modePaiementAsEnum()})
              AND (:#{#filter.fromDate()} IS NULL OR depense.dateDepense >= :#{#filter.fromDate()})
              AND (:#{#filter.toDate()} IS NULL OR depense.dateDepense <= :#{#filter.toDate()})
            ORDER BY depense.dateDepense DESC
            """)
    Page<DepenseResponse> findResponsesByFilter(@Param("filter") DepenseFilter filter,
                                                @Param("entrepriseId") UUID entrepriseId,
                                                Pageable pageable);

    @Query("""
            SELECT new org.store.depense.application.dto.DepenseTotalResponse(
                :#{#filter.magasinId},
                COALESCE(SUM(depense.montant), 0),
                COUNT(depense)
            )
            FROM Depense depense
            WHERE depense.magasin.entreprise.id = :entrepriseId
              AND depense.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.categoryId} IS NULL OR depense.category.id = :#{#filter.categoryId})
              AND (:#{#filter.modePaiementAsEnum()} IS NULL OR depense.modePaiement = :#{#filter.modePaiementAsEnum()})
              AND (:#{#filter.fromDate()} IS NULL OR depense.dateDepense >= :#{#filter.fromDate()})
              AND (:#{#filter.toDate()} IS NULL OR depense.dateDepense <= :#{#filter.toDate()})
            """)
    DepenseTotalResponse computeTotal(@Param("filter") DepenseFilter filter,
                                      @Param("entrepriseId") UUID entrepriseId);
}
