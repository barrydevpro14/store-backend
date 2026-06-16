package org.store.stock.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.stock.application.dto.MarginReportResponse;
import org.store.stock.domain.model.SortieStock;

import java.util.List;
import java.util.UUID;

public interface SortieStockRepository extends BaseRepository<SortieStock> {

    @Query("""
            SELECT new org.store.stock.application.dto.MarginReportResponse(
                COALESCE(SUM(sortie.marge), 0),
                COALESCE(SUM(CAST(sortie.quantiteSortie AS long)), 0),
                COUNT(sortie)
            )
            FROM SortieStock sortie
            JOIN sortie.entreeStock entree
            WHERE entree.magasin.entreprise.id = :entrepriseId
              AND entree.magasin.id = :magasinId
              AND sortie.annulee = false
              AND (:productId IS NULL OR entree.produit.id = :productId)
              AND (:fournisseurId IS NULL OR entree.productFournisseur.fournisseur.id = :fournisseurId)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', sortie.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', sortie.createdAt) <= CAST(:endDate AS date))
            """)
    MarginReportResponse computeMargin(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            @Param("productId") UUID productId,
            @Param("fournisseurId") UUID fournisseurId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    List<SortieStock> findAllByLigneVenteIdAndAnnuleeFalse(UUID ligneVenteId);
}
