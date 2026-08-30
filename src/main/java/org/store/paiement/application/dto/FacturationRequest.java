package org.store.paiement.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record FacturationRequest(
        @NotNull UUID moyenPaiementId,
        Set<UUID> paysIds,
        @NotBlank @Size(max = 100) String numeroFacturation
) {
}
