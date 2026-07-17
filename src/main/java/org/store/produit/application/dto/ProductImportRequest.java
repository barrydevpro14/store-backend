package org.store.produit.application.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProductImportRequest(
        @NotEmpty List<ProductImportItem> produits
) {
}
