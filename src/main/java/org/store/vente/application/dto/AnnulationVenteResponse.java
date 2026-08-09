package org.store.vente.application.dto;

import org.store.common.tools.DateHelper;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.enums.MotifAnnulationVente;
import org.store.vente.domain.model.CommandeVente;

import java.math.BigDecimal;
import java.util.UUID;

public record AnnulationVenteResponse(
        UUID commandeId,
        String reference,
        CommandeVenteStatut statut,
        MotifAnnulationVente motif,
        String commentaire,
        String dateAnnulation,
        BigDecimal totalQuantiteReinjectee,
        int nombreMouvementsCrees
) {
    public AnnulationVenteResponse(CommandeVente commande, BigDecimal totalQuantiteReinjectee, int nombreMouvementsCrees) {
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
