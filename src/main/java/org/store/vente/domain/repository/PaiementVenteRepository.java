package org.store.vente.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.vente.application.dto.PaiementParMoyenResponse;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.domain.model.PaiementVente;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PaiementVenteRepository extends BaseRepository<PaiementVente> {

    List<PaiementVente> findAllByFactureId(UUID factureId);

    @Query("""
            SELECT new org.store.vente.application.dto.PaiementVenteResponse(paiement)
            FROM PaiementVente paiement
            WHERE paiement.facture.id = :factureId
              AND paiement.facture.commande.magasin.entreprise.id = :entrepriseId
            ORDER BY paiement.datePaiement DESC
            """)
    Page<PaiementVenteResponse> findResponsesByFactureId(@Param("factureId") UUID factureId,
                                                        @Param("entrepriseId") UUID entrepriseId,
                                                        Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(paiement.montant), 0) FROM PaiementVente paiement
            WHERE paiement.facture.commande.magasin.entreprise.id = :entrepriseId
              AND paiement.facture.commande.magasin.id = :magasinId
              AND paiement.facture.commande.statut = org.store.vente.domain.enums.CommandeVenteStatut.VALIDATE
              AND paiement.createdAt >= :startOfDay
              AND paiement.createdAt <= :endOfDay
            """)
    BigDecimal sumMontantByMagasinAndDay(@Param("magasinId") UUID magasinId,
                                         @Param("entrepriseId") UUID entrepriseId,
                                         @Param("startOfDay") LocalDateTime startOfDay,
                                         @Param("endOfDay") LocalDateTime endOfDay);

    @Query("""
            SELECT new org.store.vente.application.dto.PaiementParMoyenResponse(
                paiement.moyen, SUM(paiement.montant), COUNT(paiement)
            )
            FROM PaiementVente paiement
            WHERE paiement.facture.commande.magasin.entreprise.id = :entrepriseId
              AND paiement.facture.commande.magasin.id = :magasinId
              AND paiement.facture.commande.statut = org.store.vente.domain.enums.CommandeVenteStatut.VALIDATE
              AND paiement.createdAt >= :startOfDay
              AND paiement.createdAt <= :endOfDay
            GROUP BY paiement.moyen
            ORDER BY SUM(paiement.montant) DESC
            """)
    List<PaiementParMoyenResponse> ventilationParMoyenByMagasinAndDay(
            @Param("magasinId") UUID magasinId,
            @Param("entrepriseId") UUID entrepriseId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);
}
