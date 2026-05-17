package org.store.abonnement.application.dto;

import org.store.abonnement.domain.model.PlanAbonnement;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanAbonnementResponse(
        UUID id,
        String nom,
        String description,
        BigDecimal prix,
        int nombreMagasinsMax,
        int nombreEmployesMax,
        boolean gestionStock,
        boolean gestionVente,
        boolean gestionAchat,
        boolean gestionComptabilite,
        boolean actif,
        boolean visible,
        boolean trial,
        int ordre
) {
    public PlanAbonnementResponse(PlanAbonnement plan) {
        this(
                plan.getId(),
                plan.getNom(),
                plan.getDescription(),
                plan.getPrix(),
                plan.getNombreMagasinsMax(),
                plan.getNombreEmployesMax(),
                plan.isGestionStock(),
                plan.isGestionVente(),
                plan.isGestionAchat(),
                plan.isGestionComptabilite(),
                plan.isActif(),
                plan.isVisible(),
                plan.isTrial(),
                plan.getOrdre()
        );
    }
}
