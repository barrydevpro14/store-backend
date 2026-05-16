package org.store.stock.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.stock.application.dto.ExpiringLotResponse;
import org.store.stock.application.dto.ExpiringLotsFilter;
import org.store.stock.domain.model.EntreeStock;

import java.util.List;
import java.util.UUID;

public interface EntreeStockRepository extends BaseRepository<EntreeStock> {

    @Query("""
            SELECT e FROM EntreeStock e
            WHERE e.magasin.id = :magasinId
              AND e.produit.id = :productId
              AND e.quantiteRestante > 0
            ORDER BY e.createdAt ASC
            """)
    List<EntreeStock> findAvailableLotsForFifo(@Param("magasinId") UUID magasinId, @Param("productId") UUID productId);

    @Query("""
            SELECT e FROM EntreeStock e
            WHERE e.magasin.id = :magasinId
              AND e.productFournisseur.id = :productFournisseurId
              AND e.quantiteRestante > 0
            ORDER BY e.createdAt ASC
            """)
    List<EntreeStock> findAvailableLotsForFifoByProductFournisseur(@Param("magasinId") UUID magasinId,
                                                                   @Param("productFournisseurId") UUID productFournisseurId);

    @Query("""
            SELECT new org.store.stock.application.dto.ExpiringLotResponse(e)
            FROM EntreeStock e
            WHERE e.magasin.entreprise.id = :entrepriseId
              AND e.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.productId} IS NULL OR e.produit.id = :#{#filter.productId})
              AND e.dateExpiration IS NOT NULL
              AND e.dateExpiration <= :#{#filter.untilDate()}
              AND e.quantiteRestante > 0
            ORDER BY e.dateExpiration ASC
            """)
    Page<ExpiringLotResponse> findExpiringLots(@Param("filter") ExpiringLotsFilter filter,
                                               @Param("entrepriseId") UUID entrepriseId,
                                               Pageable pageable);

    @Query("""
            SELECT e FROM EntreeStock e
            JOIN FETCH e.productFournisseur pf
            JOIN FETCH pf.fournisseur
            JOIN FETCH pf.quality
            WHERE e.magasin.id = :magasinId
              AND e.quantiteRestante > 0
              AND e.produit.id IN :productIds
            ORDER BY e.createdAt ASC
            """)
    List<EntreeStock> findActiveLotsByMagasinAndProductIds(@Param("magasinId") UUID magasinId,
                                                           @Param("productIds") List<UUID> productIds);
}
