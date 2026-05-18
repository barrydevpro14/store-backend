package org.store.achat.application.dto;

import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.MotifAnnulationAchat;
import org.store.achat.domain.model.CommandeAchat;
import org.store.common.tools.DateHelper;

import java.util.UUID;

public record AnnulationAchatResponse(
        UUID commandeId,
        String reference,
        CommandeAchatStatut statut,
        MotifAnnulationAchat motif,
        String commentaire,
        String dateAnnulation,
        int totalQuantiteRetiree,
        int nombreMouvementsCrees
) {
    public AnnulationAchatResponse(CommandeAchat commande, int totalQuantiteRetiree, int nombreMouvementsCrees) {
        this(
                commande.getId(),
                commande.getReference(),
                commande.getStatut(),
                commande.getMotifAnnulation(),
                commande.getCommentaireAnnulation(),
                DateHelper.format(commande.getDateAnnulation()),
                totalQuantiteRetiree,
                nombreMouvementsCrees
        );
    }
}
