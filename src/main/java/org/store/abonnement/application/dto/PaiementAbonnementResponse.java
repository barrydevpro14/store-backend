package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.TypePlanAbonnement;
import org.store.paiement.application.dto.MoyenPaiementResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaiementAbonnementResponse(
        UUID id,
        UUID abonnementId,
        String entrepriseSigle,
        PlanAbonnementSummaryResponse plan,
        SubscriptionTypeSummaryResponse type,
        BigDecimal montantAvantReduction,
        BigDecimal reduction,
        BigDecimal montantFinal,
        LocalDate datePaiement,
        MoyenPaiementResponse moyen,
        String referenceTransaction,
        StatutPaiementAbonnement statut,
        String motifRejet,
        UUID preuveId,
        LocalDateTime createdAt
) {
    public PaiementAbonnementResponse(PaiementAbonnement paiement) {
        this(
                paiement.getId(),
                paiement.getAbonnement().getId(),
                paiement.getAbonnement().getEntreprise() == null
                        ? null : paiement.getAbonnement().getEntreprise().getSigle(),
                planSummaryOf(paiement.getAbonnement().getTypePlanAbonnement()),
                typeSummaryOf(paiement.getAbonnement().getTypePlanAbonnement()),
                paiement.getMontantAvantReduction(),
                paiement.getReduction(),
                paiement.getMontantFinal(),
                paiement.getDatePaiement(),
                paiement.getMoyen() != null ? new MoyenPaiementResponse(paiement.getMoyen()) : null,
                paiement.getReferenceTransaction(),
                paiement.getStatut(),
                paiement.getMotifRejet(),
                paiement.getPreuve() == null ? null : paiement.getPreuve().getId(),
                paiement.getCreatedAt()
        );
    }

    private static PlanAbonnementSummaryResponse planSummaryOf(TypePlanAbonnement type) {
        if (type == null || type.getPlan() == null) {
            return null;
        }
        return new PlanAbonnementSummaryResponse(type.getPlan());
    }

    private static SubscriptionTypeSummaryResponse typeSummaryOf(TypePlanAbonnement type) {
        return type == null ? null : new SubscriptionTypeSummaryResponse(type);
    }
}
