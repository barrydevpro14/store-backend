package org.store.vente.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaiementVenteRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal montant,
        @NotNull UUID moyenPaiementId,
        @PastOrPresent LocalDate datePaiement
) {
}
