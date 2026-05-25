package org.store.stock.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.stock.application.dto.MouvementStockFilter;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.domain.model.MouvementStock;

import java.util.UUID;

public interface MouvementStockRepository extends BaseRepository<MouvementStock> {

    @Query("""
            SELECT new org.store.stock.application.dto.MouvementStockResponse(mouvement)
            FROM MouvementStock mouvement
            WHERE mouvement.stock.magasin.entreprise.id = :entrepriseId
              AND mouvement.stock.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.productId} IS NULL OR mouvement.stock.produit.id = :#{#filter.productId})
              AND (:#{#filter.stockId} IS NULL OR mouvement.stock.id = :#{#filter.stockId})
              AND (:#{#filter.typeAsEnum()} IS NULL OR mouvement.type = :#{#filter.typeAsEnum()})
              AND (:#{#filter.fromDateTime()} IS NULL OR mouvement.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR mouvement.createdAt <= :#{#filter.toDateTime()})
              AND mouvement.createdAt >= :#{#filter.createdStartDateTime()}
              AND mouvement.createdAt <  :#{#filter.createdEndDateTime()}
            ORDER BY mouvement.createdAt DESC
            """)
    Page<MouvementStockResponse> findResponsesByFilter(@Param("filter") MouvementStockFilter filter,
                                                       @Param("entrepriseId") UUID entrepriseId,
                                                       Pageable pageable);
}
