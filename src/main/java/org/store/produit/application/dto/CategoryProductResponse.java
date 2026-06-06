package org.store.produit.application.dto;

import org.store.produit.domain.model.CategoryProduct;

import java.util.UUID;

public record CategoryProductResponse(
        UUID id,
        String libelle,
        String description,
        UUID entrepriseId
) {
    public CategoryProductResponse(CategoryProduct categoryProduct) {
        this(
                categoryProduct.getId(),
                categoryProduct.getLibelle(),
                categoryProduct.getDescription(),
                categoryProduct.getEntreprise().getId()
        );
    }
}
