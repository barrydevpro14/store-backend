package org.store.vente.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LigneLivraisonRequest(
        @NotNull
        @DecimalMin("0")
        BigDecimal quantiteLivree
) {}
