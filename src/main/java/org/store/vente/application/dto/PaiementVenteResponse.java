package org.store.vente.application.dto;

import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.vente.domain.model.PaiementVente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaiementVenteResponse(
        UUID id,
        BigDecimal montant,
        LocalDate datePaiement,
        MoyenPaiementResponse moyen,
        UUID factureId
) {
    public PaiementVenteResponse(PaiementVente paiement) {
        this(
                paiement.getId(),
                paiement.getMontant(),
                paiement.getDatePaiement(),
                paiement.getMoyen() != null ? new MoyenPaiementResponse(paiement.getMoyen()) : null,
                paiement.getFacture() != null ? paiement.getFacture().getId() : null
        );
    }
}
