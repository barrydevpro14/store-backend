package org.store.achat.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record LigneReceptionRequest(
        @NotNull UUID ligneId,
        @Positive int quantite,
        @Size(max = 100) String numeroLot,
        LocalDate dateExpiration
) {
}
