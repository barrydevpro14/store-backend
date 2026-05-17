package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.achat.domain.enums.MoyenPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaiementAbonnementResponse(
        UUID id,
        UUID abonnementId,
        BigDecimal montantAvantReduction,
        BigDecimal reduction,
        BigDecimal montantFinal,
        LocalDate datePaiement,
        MoyenPaiement moyen,
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
                paiement.getMontantAvantReduction(),
                paiement.getReduction(),
                paiement.getMontantFinal(),
                paiement.getDatePaiement(),
                paiement.getMoyen(),
                paiement.getReferenceTransaction(),
                paiement.getStatut(),
                paiement.getMotifRejet(),
                paiement.getPreuve() == null ? null : paiement.getPreuve().getId(),
                paiement.getCreatedAt()
        );
    }
}
