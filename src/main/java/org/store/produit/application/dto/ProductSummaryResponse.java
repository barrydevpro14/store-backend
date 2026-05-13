package org.store.produit.application.dto;

import org.store.produit.domain.model.Product;

import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String nom,
        String reference
) {
    public ProductSummaryResponse(Product product) {
        this(product.getId(), product.getNom(), product.getReference());
    }
}
