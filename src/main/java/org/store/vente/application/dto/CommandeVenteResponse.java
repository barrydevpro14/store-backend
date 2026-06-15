package org.store.vente.application.dto;

import org.store.achat.domain.enums.StatutFacture;
import org.store.common.dto.UserSummaryResponse;
import org.store.common.tools.DateHelper;
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
        UserSummaryResponse user,
        LocalDate dateVente,
        BigDecimal montantTotal,
        BigDecimal montantPaye,
        StatutFacture statutFacture,
        String createdAt
) {
    /** Constructeur applicatif : la facture porte montantPaye et le statut ; montantTotal vient de la commande. */
    public CommandeVenteResponse(CommandeVente commande, UserSummaryResponse user, FactureClient facture) {
        this(commande, user,
                facture != null ? facture.getStatut() : null,
                commande.getMontantTotal() != null ? commande.getMontantTotal() : BigDecimal.ZERO,
                facture != null && facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO);
    }

    /** Projection JPQL listing : user null, statutFacture + montantPaye de la facture, montantTotal de la commande. */
    public CommandeVenteResponse(CommandeVente commande, StatutFacture statutFacture, BigDecimal montantPaye) {
        this(commande, null, statutFacture,
                commande.getMontantTotal() != null ? commande.getMontantTotal() : BigDecimal.ZERO,
                montantPaye);
    }

    /** Projection JPQL listing rétrocompat (sans statutFacture). */
    public CommandeVenteResponse(CommandeVente commande, BigDecimal montantPaye) {
        this(commande, (StatutFacture) null, montantPaye);
    }

    /** Projection JPQL GET by id. */
    public CommandeVenteResponse(CommandeVente commande, UUID userId, String nomComplet, BigDecimal montantPaye) {
        this(commande, userId != null ? new UserSummaryResponse(userId, nomComplet) : null, null,
                commande.getMontantTotal() != null ? commande.getMontantTotal() : BigDecimal.ZERO,
                montantPaye);
    }

    private CommandeVenteResponse(CommandeVente commande, UserSummaryResponse user,
                                  StatutFacture statutFacture,
                                  BigDecimal montantTotal, BigDecimal montantPaye) {
        this(
                commande.getId(),
                commande.getReference(),
                commande.getStatut(),
                commande.getClient() != null ? new ClientSummaryResponse(commande.getClient()) : null,
                user,
                commande.getDate(),
                montantTotal != null ? montantTotal : BigDecimal.ZERO,
                montantPaye != null ? montantPaye : BigDecimal.ZERO,
                statutFacture,
                DateHelper.format(commande.getCreatedAt())
        );
    }

    /** Montant restant à payer — sérialisé par Jackson comme champ JSON. */
    public BigDecimal montantRestant() {
        return montantTotal.subtract(montantPaye);
    }
}
