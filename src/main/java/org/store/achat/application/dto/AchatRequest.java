package org.store.achat.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AchatRequest(
        @NotNull UUID magasinId,
        @NotNull UUID fournisseurId,
        @NotNull LocalDate dateCommande,
        @NotEmpty @Valid List<LigneAchatRequest> lignes
) {
}
