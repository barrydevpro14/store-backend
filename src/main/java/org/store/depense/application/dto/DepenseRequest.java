package org.store.depense.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DepenseRequest(
        @NotNull UUID magasinId,
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 200) String libelle,
        @Size(max = 1000) String description,
        @NotNull LocalDate dateDepense,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal montant,
        @NotNull UUID moyenPaiementId
) {
}
