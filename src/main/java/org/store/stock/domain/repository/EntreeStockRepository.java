package org.store.stock.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
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
}
