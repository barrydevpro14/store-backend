package org.store.produit.application.dto;

import org.store.produit.domain.model.Product;

import java.util.UUID;

public record ProductResponse(
        UUID id,
        String nom,
        String reference,
        String description,
        CategoryProductSummaryResponse category,
        UUID entrepriseId,
        String image
) {
    public ProductResponse(Product product) {
        this(
                product.getId(),
                product.getNom(),
                product.getReference(),
                product.getDescription(),
                new CategoryProductSummaryResponse(product.getCategoryProduct()),
                product.getEntreprise().getId(),
                product.getImagePrincipal() != null ? "/api/v1/products/" + product.getId() + "/image" : null
        );
    }
}
