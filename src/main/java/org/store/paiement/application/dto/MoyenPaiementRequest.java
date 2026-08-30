package org.store.paiement.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record MoyenPaiementRequest(
        @NotBlank @Size(max = 100) String libelle,
        Set<UUID> paysIds
) {
}
