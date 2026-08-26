package org.store.abonnement.application.dto;

import org.store.abonnement.domain.model.PaiementAbonnement;

import java.util.List;
import java.util.UUID;

public record PaiementAbonnementDetailsResponse(
        PaiementAbonnementResponse facture,
        List<PreuvePaiementResponse> preuves
) {
    /** Built directly from the entity — matches this codebase's PaiementAbonnementResponse(PaiementAbonnement)
     * convention, usable in a JPQL `SELECT NEW` once `paiement.preuves` is fetch-joined. */
    public PaiementAbonnementDetailsResponse(PaiementAbonnement paiement) {
        this(
                new PaiementAbonnementResponse(paiement),
                paiement.getPreuves().stream().map(PreuvePaiementResponse::new).toList()
        );
    }

    public UUID id() {
        return facture.id();
    }
}
