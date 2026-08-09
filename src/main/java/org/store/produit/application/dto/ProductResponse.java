package org.store.produit.application.dto;

import org.store.produit.domain.model.Product;
import org.store.produit.presentation.ProductController;

import java.util.UUID;

public record ProductResponse(
        UUID id,
        String nom,
        String reference,
        String description,
        CategoryProductSummaryResponse category,
        UniteMesureSummaryResponse uniteMesure,
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
                new UniteMesureSummaryResponse(product.getUniteMesure()),
                product.getEntreprise().getId(),
                product.getImagePrincipal() != null ? ProductController.BASE_PATH + "/" + product.getId() + "/image" : null
        );
    }
}
