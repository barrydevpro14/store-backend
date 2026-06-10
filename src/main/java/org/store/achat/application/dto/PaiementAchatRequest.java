package org.store.achat.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaiementAchatRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal montant,
        @NotNull LocalDate datePaiement,
        @NotNull UUID moyenPaiementId
) {
}
