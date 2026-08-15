package org.store.abonnement.application.dto;

import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record PublicPlanResponse(
        UUID id,
        String nom,
        String description,
        int nombreMagasinsMax,
        int nombreEmployesMax,
        boolean gestionStock,
        boolean gestionVente,
        boolean gestionAchat,
        boolean gestionComptabilite,
        int ordre,
        List<PlanAbonnementTarifResponse> tarifs
) {
    public PublicPlanResponse(PlanAbonnement plan) {
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
                plan.getOrdre(),
                plan.getTarifs().stream()
                        .filter(PlanAbonnementTarif::isActif)
                        .sorted(Comparator.comparingInt(t -> t.getOrdre() != null ? t.getOrdre() : 0))
                        .map(PlanAbonnementTarifResponse::new)
                        .toList()
        );
    }
}
