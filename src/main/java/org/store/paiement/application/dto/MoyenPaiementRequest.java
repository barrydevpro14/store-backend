package org.store.paiement.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MoyenPaiementRequest(
        @NotBlank @Size(max = 100) String libelle
) {
}
