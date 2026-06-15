package org.store.stock.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.MouvementStock;

import java.util.UUID;

public interface MouvementStockRepository extends BaseRepository<MouvementStock> {

    @Query("""
            SELECT new org.store.stock.application.dto.MouvementStockResponse(mouvement)
            FROM MouvementStock mouvement
            WHERE mouvement.stock.magasin.entreprise.id = :entrepriseId
              AND mouvement.stock.magasin.id = :magasinId
              AND (:productId IS NULL OR mouvement.stock.productFournisseur.product.id = :productId)
              AND (:stockId IS NULL OR mouvement.stock.id = :stockId)
              AND (:type IS NULL OR mouvement.type = :type)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', mouvement.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', mouvement.createdAt) <= CAST(:endDate AS date))
            ORDER BY mouvement.createdAt DESC
            """)
    Page<MouvementStockResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            @Param("productId") UUID productId,
            @Param("stockId") UUID stockId,
            @Param("type") MouvementStockType type,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);
}
