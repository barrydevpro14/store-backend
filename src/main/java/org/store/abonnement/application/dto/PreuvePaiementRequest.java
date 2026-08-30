package org.store.abonnement.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PreuvePaiementRequest(
        @NotNull UUID facturationId,
        @NotBlank @Size(max = 255) String referenceTransaction
) {
}
