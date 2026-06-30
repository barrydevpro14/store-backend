package org.store.stock.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.domain.model.Stock;

import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends BaseRepository<Stock> {

    Optional<Stock> findByMagasinIdAndProductFournisseurId(UUID magasinId, UUID productFournisseurId);

    @Query("""
            SELECT new org.store.stock.application.dto.StockResponse(stock)
            FROM Stock stock
            WHERE stock.magasin.entreprise.id = :entrepriseId
              AND stock.magasin.id = :magasinId
              AND (:productName IS NULL OR :productName = ''
                   OR LOWER(stock.productFournisseur.product.nom) LIKE :productNamePattern
                   OR LOWER(COALESCE(stock.productFournisseur.product.reference, '')) LIKE :productNamePattern
                   OR LOWER(stock.productFournisseur.product.categoryProduct.libelle) LIKE :productNamePattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', stock.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', stock.createdAt) <= CAST(:endDate AS date))
            ORDER BY stock.createdAt DESC
            """)
    Page<StockResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            @Param("productName") String productName,
            @Param("productNamePattern") String productNamePattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query("""
            SELECT new org.store.stock.application.dto.StockResponse(stock)
            FROM Stock stock
            WHERE stock.magasin.entreprise.id = :entrepriseId
              AND stock.magasin.id = :magasinId
              AND stock.seuilApprovisionnement > 0
              AND stock.quantiteDisponible <= stock.seuilApprovisionnement
            """)
    Page<StockResponse> findResponsesBelowThreshold(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(stock)
            FROM Stock stock
            WHERE stock.magasin.id = :magasinId
              AND stock.seuilApprovisionnement > 0
              AND stock.quantiteDisponible <= stock.seuilApprovisionnement
            """)
    long countBelowThreshold(@Param("magasinId") UUID magasinId);

    @Query("""
            SELECT COUNT(stock)
            FROM Stock stock
            WHERE stock.magasin.entreprise.id = :entrepriseId
              AND stock.seuilApprovisionnement > 0
              AND stock.quantiteDisponible <= stock.seuilApprovisionnement
            """)
    long countBelowThresholdByEntreprise(@Param("entrepriseId") UUID entrepriseId);

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
