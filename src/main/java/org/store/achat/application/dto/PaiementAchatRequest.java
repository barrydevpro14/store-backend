package org.store.achat.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.store.achat.domain.enums.MoyenPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaiementAchatRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal montant,
        @NotNull LocalDate datePaiement,
        @NotNull MoyenPaiement moyen
) {
}
