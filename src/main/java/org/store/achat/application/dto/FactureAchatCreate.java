package org.store.achat.application.dto;

import org.store.achat.domain.model.CommandeAchat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FactureAchatCreate(
        CommandeAchat commande,
        String numero,
        LocalDate date,
        LocalDate dateEcheance,
        BigDecimal montantTotal
) {
}
