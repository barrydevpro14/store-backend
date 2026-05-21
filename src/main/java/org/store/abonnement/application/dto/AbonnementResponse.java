package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.TypePlanAbonnement;

import java.time.LocalDate;
import java.util.UUID;

public record AbonnementResponse(
        UUID id,
        UUID entrepriseId,
        String entrepriseSigle,
        PlanAbonnementSummaryResponse plan,
        SubscriptionTypeSummaryResponse type,
        LocalDate dateDebut,
        LocalDate dateFin,
        boolean actif,
        boolean renouvellementAuto,
        AbonnementStatut statut
) {
    public AbonnementResponse(Abonnement abonnement) {
        this(
                abonnement.getId(),
                abonnement.getEntreprise().getId(),
                abonnement.getEntreprise().getSigle(),
                planSummary(abonnement.getTypePlanAbonnement()),
                new SubscriptionTypeSummaryResponse(abonnement.getTypePlanAbonnement()),
                abonnement.getDateDebut(),
                abonnement.getDateFin(),
                abonnement.isActif(),
                abonnement.isRenouvellementAuto(),
                abonnement.getStatut()
        );
    }

    private static PlanAbonnementSummaryResponse planSummary(TypePlanAbonnement type) {
        return new PlanAbonnementSummaryResponse(type.getPlan());
    }
}
