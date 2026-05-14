package org.store.achat.application.dto;

import org.store.achat.domain.enums.MoyenPaiement;
import org.store.achat.domain.model.FactureAchat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaiementAchatCreate(
        FactureAchat facture,
        BigDecimal montant,
        LocalDate datePaiement,
        MoyenPaiement moyen
) {
}
