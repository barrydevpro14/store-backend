package org.store.vente.application.dto;

import org.store.vente.domain.model.CommandeVente;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FactureClientCreate(
        CommandeVente commande,
        String numero,
        LocalDate date,
        LocalDate dateEcheance,
        BigDecimal montantTotal
) {
}
