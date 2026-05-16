package org.store.vente.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VenteRequest(
        UUID clientId,
        @PastOrPresent LocalDate dateVente,
        @NotNull @FutureOrPresent LocalDate dateEcheance,
        @NotEmpty @Valid List<LigneVenteRequest> lignes,
        @Valid PaiementVenteRequest premierPaiement
) {
}
