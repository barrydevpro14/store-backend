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
            SELECT entree FROM EntreeStock entree
            WHERE entree.magasin.id = :magasinId
              AND entree.produit.id = :productId
              AND entree.quantiteRestante > 0
            ORDER BY entree.createdAt ASC
            """)
    List<EntreeStock> findAvailableLotsForFifo(@Param("magasinId") UUID magasinId, @Param("productId") UUID productId);

    @Query("""
            SELECT entree FROM EntreeStock entree
            WHERE entree.magasin.id = :magasinId
              AND entree.productFournisseur.id = :productFournisseurId
              AND entree.quantiteRestante > 0
            ORDER BY entree.createdAt ASC
            """)
    List<EntreeStock> findAvailableLotsForFifoByProductFournisseur(@Param("magasinId") UUID magasinId,
                                                                   @Param("productFournisseurId") UUID productFournisseurId);

    @Query("""
            SELECT new org.store.stock.application.dto.ExpiringLotResponse(entree)
            FROM EntreeStock entree
            WHERE entree.magasin.entreprise.id = :entrepriseId
              AND entree.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.productId} IS NULL OR entree.produit.id = :#{#filter.productId})
              AND entree.dateExpiration IS NOT NULL
              AND entree.dateExpiration <= :#{#filter.untilDate()}
              AND entree.quantiteRestante > 0
            ORDER BY entree.dateExpiration ASC
            """)
    Page<ExpiringLotResponse> findExpiringLots(@Param("filter") ExpiringLotsFilter filter,
                                               @Param("entrepriseId") UUID entrepriseId,
                                               Pageable pageable);

    @Query("""
            SELECT entree FROM EntreeStock entree
            JOIN FETCH entree.productFournisseur productFournisseur
            JOIN FETCH productFournisseur.fournisseur
            JOIN FETCH productFournisseur.quality
            WHERE entree.magasin.id = :magasinId
              AND entree.quantiteRestante > 0
              AND entree.produit.id IN :productIds
            ORDER BY entree.createdAt ASC
            """)
    List<EntreeStock> findActiveLotsByMagasinAndProductIds(@Param("magasinId") UUID magasinId,
                                                           @Param("productIds") List<UUID> productIds);

    @Query("""
            SELECT entree FROM EntreeStock entree
            WHERE entree.commandeAchat.id = :commandeAchatId
            """)
    List<EntreeStock> findByCommandeAchatId(@Param("commandeAchatId") UUID commandeAchatId);
}
