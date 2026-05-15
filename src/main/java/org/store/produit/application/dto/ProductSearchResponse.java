package org.store.produit.application.dto;

import org.store.produit.domain.model.Product;

import java.util.List;
import java.util.UUID;

public record ProductSearchResponse(
        UUID id,
        String nom,
        String reference,
        String description,
        CategoryProductSummaryResponse category,
        String image,
        Integer quantiteEnStock,
        List<ProductFournisseurStockResponse> productFournisseurs
) {
    public ProductSearchResponse(Product product, Integer quantiteEnStock, List<ProductFournisseurStockResponse> productFournisseurs) {
        this(
                product.getId(),
                product.getNom(),
                product.getReference(),
                product.getDescription(),
                new CategoryProductSummaryResponse(product.getCategoryProduct()),
                product.getImagePrincipal() != null ? "/api/v1/products/" + product.getId() + "/image" : null,
                quantiteEnStock,
                productFournisseurs
        );
    }
}
