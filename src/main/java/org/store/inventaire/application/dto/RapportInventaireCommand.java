package org.store.inventaire.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RapportInventaireCommand(
        BigDecimal montantAutomatique,
        BigDecimal montantPhysique,
        BigDecimal montantCaisse,
        BigDecimal depense,
        BigDecimal montantRoulement,
        LocalDate dateDebutPeriode,
        LocalDate dateFinPeriode
) {
}
