package org.store.abonnement.application.dto;

import org.store.abonnement.domain.model.PlanAbonnement;

import java.util.UUID;

public record PlanAbonnementResponse(
        UUID id,
        String nom,
        String description,
        int nombreMagasinsMax,
        int nombreEmployesMax,
        boolean gestionStock,
        boolean gestionVente,
        boolean gestionAchat,
        boolean gestionComptabilite,
        boolean actif,
        boolean visible,
        int ordre
) {
    public PlanAbonnementResponse(PlanAbonnement plan) {
        this(
                plan.getId(),
                plan.getNom(),
                plan.getDescription(),
                plan.getNombreMagasinsMax(),
                plan.getNombreEmployesMax(),
                plan.isGestionStock(),
                plan.isGestionVente(),
                plan.isGestionAchat(),
                plan.isGestionComptabilite(),
                plan.isActif(),
                plan.isVisible(),
                plan.getOrdre()
        );
    }
}
