package org.store.inventaire.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LigneInventaireUpdateRequest(
        @NotNull @DecimalMin("0") BigDecimal quantiteReelle
) {
}
