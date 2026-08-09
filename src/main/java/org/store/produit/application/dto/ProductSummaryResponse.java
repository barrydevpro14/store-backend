package org.store.produit.application.dto;

import org.store.produit.domain.model.Product;

import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String nom,
        String reference,
        String categoryLibelle,
        String uniteMesure
) {
    public ProductSummaryResponse(Product product) {
        this(
                product.getId(),
                product.getNom(),
                product.getReference(),
                product.getCategoryProduct() != null ? product.getCategoryProduct().getLibelle() : null,
                product.getUniteMesure() != null ? product.getUniteMesure().getSymbole() : ""
        );
    }
}
