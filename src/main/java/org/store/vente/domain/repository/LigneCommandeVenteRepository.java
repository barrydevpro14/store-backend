package org.store.vente.domain.repository;

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

    @Query("""
            SELECT new org.store.vente.application.dto.TopProduitResponse(
                p.id, p.nom, p.reference,
                SUM(l.quantite),
                COALESCE(SUM(l.montantTotal), 0)
            )
            FROM LigneCommandeVente l
            JOIN l.product p
            JOIN l.commande c
            WHERE c.magasin.entreprise.id = :entrepriseId
              AND c.magasin.id = :magasinId
              AND c.createdAt >= :startOfDay
              AND c.createdAt <= :endOfDay
            GROUP BY p.id, p.nom, p.reference
            ORDER BY SUM(l.quantite) DESC
            """)
    List<TopProduitResponse> findTopProduitsByMagasinAndDay(@Param("magasinId") UUID magasinId,
                                                            @Param("entrepriseId") UUID entrepriseId,
                                                            @Param("startOfDay") LocalDateTime startOfDay,
                                                            @Param("endOfDay") LocalDateTime endOfDay,
                                                            Pageable pageable);
}
