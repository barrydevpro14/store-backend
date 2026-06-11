package org.store.abonnement.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record PaiementAbonnementRequest(
        @NotNull UUID moyenPaiementId,
        @Size(max = 255) String referenceTransaction,
        @NotNull @PastOrPresent LocalDate datePaiement
) {
}
