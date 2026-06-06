package org.store.inventaire.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LigneInventaireUpdateRequest(
        @NotNull @Min(0) Integer quantiteReelle
) {
}
