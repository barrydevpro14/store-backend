package org.store.produit.application.dto;

import org.store.produit.domain.model.Product;

import java.util.UUID;

public record ProductResponse(
        UUID id,
        String nom,
        String reference,
        String description,
        CategoryProductResponse category,
        QualityResponse quality,
        UUID entrepriseId,
        UUID imagePrincipalId
) {
    public ProductResponse(Product product) {
        this(
                product.getId(),
                product.getNom(),
                product.getReference(),
                product.getDescription(),
                new CategoryProductResponse(product.getCategoryProduct()),
                new QualityResponse(product.getQuality()),
                product.getEntreprise().getId(),
                product.getImagePrincipal() != null ? product.getImagePrincipal().getId() : null
        );
    }
}
