package org.store.achat.application.dto;

import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.CommandeAchat;
import org.store.common.tools.DateHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CommandeAchatResponse(
        UUID id,
        String reference,
        CommandeAchatStatut statut,
        FournisseurSummaryResponse fournisseur,
        LocalDate dateCommande,
        List<LigneCommandeAchatResponse> lignes,
        StatutFacture statutFacture,
        String createdAt,
        BigDecimal montantTotal
) {
    /** Constructeur unique — lit facture et montantTotal depuis la commande. */
    public CommandeAchatResponse(CommandeAchat commande) {
        this(
                commande.getId(),
                commande.getReference(),
                commande.getStatut(),
                new FournisseurSummaryResponse(commande.getFournisseur()),
                commande.getDate(),
                List.of(),
                commande.getFacture() != null ? commande.getFacture().getStatut() : null,
                DateHelper.format(commande.getCreatedAt()),
                commande.getMontantTotal() != null ? commande.getMontantTotal() : BigDecimal.ZERO
        );
    }
}
