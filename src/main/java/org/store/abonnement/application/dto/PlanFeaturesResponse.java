package org.store.abonnement.application.dto;

import org.store.abonnement.domain.model.PlanAbonnement;

public record PlanFeaturesResponse(
        boolean gestionStock,
        boolean gestionVente,
        boolean gestionAchat,
        boolean gestionComptabilite,
        int nombreMagasinsMax,
        int nombreEmployesMax
) {
    public PlanFeaturesResponse(PlanAbonnement plan) {
        this(
                plan.isGestionStock(),
                plan.isGestionVente(),
                plan.isGestionAchat(),
                plan.isGestionComptabilite(),
                plan.getNombreMagasinsMax(),
                plan.getNombreEmployesMax()
        );
    }
}
