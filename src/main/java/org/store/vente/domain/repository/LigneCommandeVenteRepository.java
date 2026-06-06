package org.store.vente.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.vente.application.dto.TopProduitResponse;
import org.store.vente.domain.model.LigneCommandeVente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LigneCommandeVenteRepository extends BaseRepository<LigneCommandeVente> {

    @Query("SELECT l FROM LigneCommandeVente l WHERE l.commande.id = :commandeId ORDER BY l.id ASC")
    Page<LigneCommandeVente> findPagedByCommandeId(@Param("commandeId") UUID commandeId, Pageable pageable);

    @Query("""
            SELECT new org.store.vente.application.dto.TopProduitResponse(
                produit.id, produit.nom, produit.reference,
                SUM(ligne.quantite),
                COALESCE(SUM(ligne.montantTotal), 0)
            )
            FROM LigneCommandeVente ligne
            JOIN ligne.product produit
            JOIN ligne.commande commande
            WHERE commande.magasin.entreprise.id = :entrepriseId
              AND commande.magasin.id = :magasinId
              AND commande.statut = org.store.vente.domain.enums.CommandeVenteStatut.VALIDATE
              AND commande.createdAt >= :startOfDay
              AND commande.createdAt <= :endOfDay
            GROUP BY produit.id, produit.nom, produit.reference
            ORDER BY SUM(ligne.quantite) DESC
            """)
    List<TopProduitResponse> findTopProduitsByMagasinAndDay(@Param("magasinId") UUID magasinId,
                                                            @Param("entrepriseId") UUID entrepriseId,
                                                            @Param("startOfDay") LocalDateTime startOfDay,
                                                            @Param("endOfDay") LocalDateTime endOfDay,
                                                            Pageable pageable);
}
