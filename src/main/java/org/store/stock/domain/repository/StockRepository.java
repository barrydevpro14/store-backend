package org.store.stock.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.stock.application.dto.StockFilter;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.domain.model.Stock;

import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends BaseRepository<Stock> {

    Optional<Stock> findByMagasinIdAndProduitId(UUID magasinId, UUID produitId);

    @Query("""
            SELECT new org.store.stock.application.dto.StockResponse(stock)
            FROM Stock stock
            WHERE stock.magasin.entreprise.id = :entrepriseId
              AND stock.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.productId} IS NULL OR stock.produit.id = :#{#filter.productId})
              AND (:#{#filter.productNamePattern()} IS NULL
                   OR LOWER(stock.produit.nom) LIKE :#{#filter.productNamePattern()}
                   OR LOWER(COALESCE(stock.produit.reference, '')) LIKE :#{#filter.productNamePattern()})
              AND stock.createdAt >= :#{#filter.createdStartDateTime()}
              AND stock.createdAt <  :#{#filter.createdEndDateTime()}
            ORDER BY stock.createdAt DESC
            """)
    Page<StockResponse> findResponsesByFilter(@Param("filter") StockFilter filter,
                                              @Param("entrepriseId") UUID entrepriseId,
                                              Pageable pageable);

    @Query("""
            SELECT new org.store.stock.application.dto.StockResponse(stock)
            FROM Stock stock
            WHERE stock.magasin.entreprise.id = :entrepriseId
              AND stock.magasin.id = :#{#filter.magasinId}
              AND stock.seuilApprovisionnement > 0
              AND stock.quantiteDisponible <= stock.seuilApprovisionnement
            """)
    Page<StockResponse> findResponsesBelowThreshold(@Param("filter") StockFilter filter,
                                                    @Param("entrepriseId") UUID entrepriseId,
                                                    Pageable pageable);

    @Query("""
            SELECT new org.store.stock.application.dto.StockValuationResponse(
                :magasinId,
                SUM(stock.quantiteDisponible * COALESCE(stock.prixAchatMoyen, 0)),
                COUNT(stock)
            )
            FROM Stock stock
            WHERE stock.magasin.entreprise.id = :entrepriseId
              AND stock.magasin.id = :magasinId
            """)
    StockValuationResponse computeValuation(@Param("entrepriseId") UUID entrepriseId,
                                            @Param("magasinId") UUID magasinId);
}
