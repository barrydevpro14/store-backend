package org.store.vente.application.dto;

import org.store.common.tools.DateHelper;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.enums.MotifAnnulationVente;
import org.store.vente.domain.model.CommandeVente;

import java.util.UUID;

public record AnnulationVenteResponse(
        UUID commandeId,
        String reference,
        CommandeVenteStatut statut,
        MotifAnnulationVente motif,
        String commentaire,
        String dateAnnulation,
        int totalQuantiteReinjectee,
        int nombreMouvementsCrees
) {
    public AnnulationVenteResponse(CommandeVente commande, int totalQuantiteReinjectee, int nombreMouvementsCrees) {
        this(
                commande.getId(),
                commande.getReference(),
                commande.getStatut(),
                commande.getMotifAnnulation(),
                commande.getCommentaireAnnulation(),
                DateHelper.format(commande.getDateAnnulation()),
                totalQuantiteReinjectee,
                nombreMouvementsCrees
        );
    }
}
