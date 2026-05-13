package org.store.produit.application.dto;

import org.store.produit.domain.model.CategoryProduct;

import java.util.UUID;

public record CategoryProductSummaryResponse(
        UUID id,
        String libelle
) {
    public CategoryProductSummaryResponse(CategoryProduct categoryProduct) {
        this(categoryProduct.getId(), categoryProduct.getLibelle());
    }
}
