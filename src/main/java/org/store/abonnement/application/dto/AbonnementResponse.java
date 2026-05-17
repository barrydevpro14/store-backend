package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;

import java.time.LocalDate;
import java.util.UUID;

public record AbonnementResponse(
        UUID id,
        UUID entrepriseId,
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
                abonnement.getPlan() == null ? null : new PlanAbonnementSummaryResponse(abonnement.getPlan()),
                abonnement.getTypeAbonnement() == null ? null : new SubscriptionTypeSummaryResponse(abonnement.getTypeAbonnement()),
                abonnement.getDateDebut(),
                abonnement.getDateFin(),
                abonnement.isActif(),
                abonnement.isRenouvellementAuto(),
                abonnement.getStatut()
        );
    }
}
