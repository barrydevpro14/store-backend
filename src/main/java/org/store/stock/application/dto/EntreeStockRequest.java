package org.store.stock.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record EntreeStockRequest(
        @NotNull UUID magasinId,
        @NotNull UUID fournisseurId,
        @NotEmpty @Valid List<LigneEntreeStockRequest> lignes
) {
}
