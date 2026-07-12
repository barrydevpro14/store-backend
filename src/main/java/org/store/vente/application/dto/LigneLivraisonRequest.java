package org.store.vente.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LigneLivraisonRequest(
        @NotNull
        @Min(0)
        Integer quantiteLivree
) {}
