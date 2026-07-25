package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;

import java.time.LocalDate;
import java.util.UUID;

public record AbonnementResponse(
        UUID id,
        UUID entrepriseId,
        String entrepriseSigle,
        PlanAbonnementSummaryResponse plan,
        LocalDate dateDebut,
        LocalDate dateFin,
        AbonnementStatut statut
) {
    public AbonnementResponse(Abonnement abonnement) {
        this(
                abonnement.getId(),
                abonnement.getEntreprise().getId(),
                abonnement.getEntreprise().getSigle(),
                new PlanAbonnementSummaryResponse(abonnement.getPlanAbonnement()),
                abonnement.getDateDebut(),
                abonnement.getDateFin(),
                abonnement.getStatut()
        );
    }
}
