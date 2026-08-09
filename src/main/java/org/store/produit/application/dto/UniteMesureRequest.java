package org.store.produit.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UniteMesureRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 100) String libelle,
        @NotBlank @Size(max = 10) String symbole
) {
}
