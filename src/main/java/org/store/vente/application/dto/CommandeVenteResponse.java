package org.store.vente.application.dto;

import org.store.achat.domain.enums.StatutFacture;
import org.store.common.dto.UserSummaryResponse;
import org.store.common.tools.DateHelper;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.model.FactureClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CommandeVenteResponse(
        UUID id,
        String reference,
        CommandeVenteStatut statut,
        ClientSummaryResponse client,
        MagasinSummaryResponse magasin,
        UserSummaryResponse user,
        LocalDate dateVente,
        BigDecimal montantTotal,
        BigDecimal montantPaye,
        StatutFacture statutFacture,
        String createdAt
) {
    /** Constructeur applicatif : la facture porte les montants et le statut. */
    public CommandeVenteResponse(CommandeVente commande, UserSummaryResponse user, FactureClient facture) {
        this(commande, user,
                facture != null ? facture.getStatut() : null,
                facture != null ? facture.getMontantTotal() : BigDecimal.ZERO,
                facture != null && facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO);
    }

    /** Projection JPQL listing : user null, montants + statutFacture viennent de FactureClient via JOIN. */
    public CommandeVenteResponse(CommandeVente commande, StatutFacture statutFacture,
                                 BigDecimal montantTotal, BigDecimal montantPaye) {
        this(commande, null, statutFacture, montantTotal, montantPaye);
    }

    /** Projection JPQL listing (sans statutFacture — rétrocompat). */
    public CommandeVenteResponse(CommandeVente commande, BigDecimal montantTotal, BigDecimal montantPaye) {
        this(commande, (StatutFacture) null, montantTotal, montantPaye);
    }

    /** Projection JPQL GET by id : user résolu via CAST/JOIN sur Account, montants via JOIN sur FactureClient. */
    public CommandeVenteResponse(CommandeVente commande, UUID userId, String nomComplet,
                                 BigDecimal montantTotal, BigDecimal montantPaye) {
        this(commande, userId != null ? new UserSummaryResponse(userId, nomComplet) : null, null, montantTotal, montantPaye);
    }

    private CommandeVenteResponse(CommandeVente commande, UserSummaryResponse user,
                                  StatutFacture statutFacture,
                                  BigDecimal montantTotal, BigDecimal montantPaye) {
        this(
                commande.getId(),
                commande.getReference(),
                commande.getStatut(),
                commande.getClient() != null ? new ClientSummaryResponse(commande.getClient()) : null,
                new MagasinSummaryResponse(commande.getMagasin()),
                user,
                commande.getDate(),
                montantTotal != null ? montantTotal : BigDecimal.ZERO,
                montantPaye != null ? montantPaye : BigDecimal.ZERO,
                statutFacture,
                DateHelper.format(commande.getCreatedAt())
        );
    }
}
