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
            SELECT new org.store.depense.application.dto.DepenseResponse(d)
            FROM Depense d
            WHERE d.magasin.entreprise.id = :entrepriseId
              AND d.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.categoryId} IS NULL OR d.category.id = :#{#filter.categoryId})
              AND (:#{#filter.modePaiementAsEnum()} IS NULL OR d.modePaiement = :#{#filter.modePaiementAsEnum()})
              AND (:#{#filter.fromDate()} IS NULL OR d.dateDepense >= :#{#filter.fromDate()})
              AND (:#{#filter.toDate()} IS NULL OR d.dateDepense <= :#{#filter.toDate()})
            ORDER BY d.dateDepense DESC
            """)
    Page<DepenseResponse> findResponsesByFilter(@Param("filter") DepenseFilter filter,
                                                @Param("entrepriseId") UUID entrepriseId,
                                                Pageable pageable);

    @Query("""
            SELECT new org.store.depense.application.dto.DepenseTotalResponse(
                :#{#filter.magasinId},
                COALESCE(SUM(d.montant), 0),
                COUNT(d)
            )
            FROM Depense d
            WHERE d.magasin.entreprise.id = :entrepriseId
              AND d.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.categoryId} IS NULL OR d.category.id = :#{#filter.categoryId})
              AND (:#{#filter.modePaiementAsEnum()} IS NULL OR d.modePaiement = :#{#filter.modePaiementAsEnum()})
              AND (:#{#filter.fromDate()} IS NULL OR d.dateDepense >= :#{#filter.fromDate()})
              AND (:#{#filter.toDate()} IS NULL OR d.dateDepense <= :#{#filter.toDate()})
            """)
    DepenseTotalResponse computeTotal(@Param("filter") DepenseFilter filter,
                                      @Param("entrepriseId") UUID entrepriseId);
}
