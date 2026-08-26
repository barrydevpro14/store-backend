package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaiementAbonnementResponse(
        UUID id,
        UUID abonnementId,
        String entrepriseSigle,
        PlanAbonnementSummaryResponse plan,
        BigDecimal montantAvantReduction,
        BigDecimal reduction,
        BigDecimal montantFinal,
        LocalDate dateEcheance,
        LocalDate datePaiement,
        StatutPaiementAbonnement statut,
        LocalDateTime createdAt
) {
    public PaiementAbonnementResponse(PaiementAbonnement paiement) {
        this(
                paiement.getId(),
                paiement.getAbonnement().getId(),
                paiement.getAbonnement().getEntreprise() == null
                        ? null : paiement.getAbonnement().getEntreprise().getSigle(),
                planSummaryOf(paiement),
                paiement.getMontantAvantReduction(),
                paiement.getReduction(),
                paiement.getMontantFinal(),
                paiement.getDateEcheance(),
                paiement.getDatePaiement(),
                paiement.getStatut(),
                paiement.getCreatedAt()
        );
    }

    private static PlanAbonnementSummaryResponse planSummaryOf(PaiementAbonnement paiement) {
        var plan = paiement.getAbonnement().getPlanAbonnement();
        return plan == null ? null : new PlanAbonnementSummaryResponse(plan);
    }
}
