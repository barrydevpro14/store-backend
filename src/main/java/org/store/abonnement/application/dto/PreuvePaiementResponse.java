package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.StatutPreuvePaiement;
import org.store.abonnement.domain.model.PreuvePaiement;
import org.store.paiement.application.dto.MoyenPaiementResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PreuvePaiementResponse(
        UUID id,
        UUID paiementAbonnementId,
        LocalDate date,
        MoyenPaiementResponse moyen,
        String referenceTransaction,
        UUID preuveId,
        StatutPreuvePaiement statut,
        String motifRejet,
        LocalDateTime createdAt
) {
    public PreuvePaiementResponse(PreuvePaiement preuve) {
        this(
                preuve.getId(),
                preuve.getPaiementAbonnement().getId(),
                preuve.getDate(),
                preuve.getMoyen() != null ? new MoyenPaiementResponse(preuve.getMoyen()) : null,
                preuve.getReferenceTransaction(),
                preuve.getPreuve() == null ? null : preuve.getPreuve().getId(),
                preuve.getStatut(),
                preuve.getMotifRejet(),
                preuve.getCreatedAt()
        );
    }
}
