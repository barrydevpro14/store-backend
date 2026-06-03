package org.store.achat.application.dto;

import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.CommandeAchat;
import org.store.common.tools.DateHelper;
import org.store.magasin.application.dto.MagasinSummaryResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CommandeAchatResponse(
        UUID id,
        String reference,
        CommandeAchatStatut statut,
        FournisseurSummaryResponse fournisseur,
        MagasinSummaryResponse magasin,
        LocalDate dateCommande,
        List<LigneCommandeAchatResponse> lignes,
        StatutFacture statutFacture,
        String createdAt
) {
    /** Projection JPQL listing avec statutFacture. */
    public CommandeAchatResponse(CommandeAchat commande, StatutFacture statutFacture) {
        this(
                commande.getId(),
                commande.getReference(),
                commande.getStatut(),
                new FournisseurSummaryResponse(commande.getFournisseur()),
                new MagasinSummaryResponse(commande.getMagasin()),
                commande.getDate(),
                List.of(),
                statutFacture,
                DateHelper.format(commande.getCreatedAt())
        );
    }

    /** Constructeur rétrocompat (detail view avec lignes, sans statutFacture). */
    public CommandeAchatResponse(CommandeAchat commande) {
        this(
                commande.getId(),
                commande.getReference(),
                commande.getStatut(),
                new FournisseurSummaryResponse(commande.getFournisseur()),
                new MagasinSummaryResponse(commande.getMagasin()),
                commande.getDate(),
                commande.getLignes() != null
                        ? commande.getLignes().stream().map(LigneCommandeAchatResponse::new).toList()
                        : List.of(),
                null,
                DateHelper.format(commande.getCreatedAt())
        );
    }
}
