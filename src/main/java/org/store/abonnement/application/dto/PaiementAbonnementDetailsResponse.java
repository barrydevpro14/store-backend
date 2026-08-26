package org.store.abonnement.application.dto;

import java.util.List;
import java.util.UUID;

public record PaiementAbonnementDetailsResponse(
        PaiementAbonnementResponse facture,
        List<PreuvePaiementResponse> preuves
) {
    public UUID id() {
        return facture.id();
    }
}
