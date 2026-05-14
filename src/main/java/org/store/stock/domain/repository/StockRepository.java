package org.store.stock.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.stock.application.dto.StockFilter;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.domain.model.Stock;

import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends BaseRepository<Stock> {

    Optional<Stock> findByMagasinIdAndProduitId(UUID magasinId, UUID produitId);

    @Query("""
            SELECT new org.store.stock.application.dto.StockResponse(s)
            FROM Stock s
            WHERE s.magasin.entreprise.id = :entrepriseId
              AND s.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.productId} IS NULL OR s.produit.id = :#{#filter.productId})
            """)
    Page<StockResponse> findResponsesByFilter(@Param("filter") StockFilter filter,
                                              @Param("entrepriseId") UUID entrepriseId,
                                              Pageable pageable);
}
