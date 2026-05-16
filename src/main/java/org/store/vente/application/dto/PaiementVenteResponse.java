package org.store.vente.application.dto;

import org.store.achat.domain.enums.MoyenPaiement;
import org.store.vente.domain.model.PaiementVente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaiementVenteResponse(
        UUID id,
        BigDecimal montant,
        LocalDate datePaiement,
        MoyenPaiement moyen,
        UUID factureId
) {
    public PaiementVenteResponse(PaiementVente paiement) {
        this(
                paiement.getId(),
                paiement.getMontant(),
                paiement.getDatePaiement(),
                paiement.getMoyen(),
                paiement.getFacture() != null ? paiement.getFacture().getId() : null
        );
    }
}
