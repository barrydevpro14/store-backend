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
            SELECT new org.store.stock.application.dto.MouvementStockResponse(m)
            FROM MouvementStock m
            WHERE m.stock.magasin.entreprise.id = :entrepriseId
              AND m.stock.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.productId} IS NULL OR m.stock.produit.id = :#{#filter.productId})
              AND (:#{#filter.stockId} IS NULL OR m.stock.id = :#{#filter.stockId})
              AND (:#{#filter.typeAsEnum()} IS NULL OR m.type = :#{#filter.typeAsEnum()})
              AND (:#{#filter.fromDateTime()} IS NULL OR m.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR m.createdAt <= :#{#filter.toDateTime()})
            """)
    Page<MouvementStockResponse> findResponsesByFilter(@Param("filter") MouvementStockFilter filter,
                                                       @Param("entrepriseId") UUID entrepriseId,
                                                       Pageable pageable);
}
