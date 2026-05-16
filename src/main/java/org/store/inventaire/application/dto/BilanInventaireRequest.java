package org.store.inventaire.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BilanInventaireRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal montantCaisse,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal montantRoulement,
        @NotNull @PastOrPresent LocalDate dateDebutPeriode
) {
}
