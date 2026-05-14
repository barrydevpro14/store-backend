package org.store.stock.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.stock.application.dto.MarginReportFilter;
import org.store.stock.application.dto.MarginReportResponse;
import org.store.stock.domain.model.SortieStock;

import java.util.UUID;

public interface SortieStockRepository extends BaseRepository<SortieStock> {

    @Query("""
            SELECT new org.store.stock.application.dto.MarginReportResponse(
                COALESCE(SUM(s.marge), 0),
                COALESCE(SUM(CAST(s.quantiteSortie AS long)), 0),
                COUNT(s)
            )
            FROM SortieStock s
            JOIN s.entreeStock e
            WHERE e.magasin.entreprise.id = :entrepriseId
              AND e.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.productId} IS NULL OR e.produit.id = :#{#filter.productId})
              AND (:#{#filter.fournisseurId} IS NULL OR e.productFournisseur.fournisseur.id = :#{#filter.fournisseurId})
              AND (:#{#filter.fromDateTime()} IS NULL OR s.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR s.createdAt <= :#{#filter.toDateTime()})
            """)
    MarginReportResponse computeMargin(@Param("filter") MarginReportFilter filter,
                                       @Param("entrepriseId") UUID entrepriseId);
}
