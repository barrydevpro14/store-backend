package org.store.achat.application.dto;

import org.store.achat.domain.enums.MoyenPaiement;
import org.store.achat.domain.model.PaiementAchat;
import org.store.common.tools.DateHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaiementAchatResponse(
        UUID id,
        UUID factureId,
        BigDecimal montant,
        LocalDate datePaiement,
        MoyenPaiement moyen,
        String createdAt
) {
    public PaiementAchatResponse(PaiementAchat paiement) {
        this(
                paiement.getId(),
                paiement.getFacture() != null ? paiement.getFacture().getId() : null,
                paiement.getMontant(),
                paiement.getDatePaiement(),
                paiement.getMoyen(),
                DateHelper.format(paiement.getCreatedAt())
        );
    }
}
