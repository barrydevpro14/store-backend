package org.store.inventaire.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LigneInventaireRequest(
        @NotNull UUID productFournisseurId,
        @NotNull @Min(0) Integer quantiteReelle
) {
}
