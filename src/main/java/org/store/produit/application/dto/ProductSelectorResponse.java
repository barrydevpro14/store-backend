package org.store.produit.application.dto;

import org.store.produit.domain.model.Product;

import java.util.UUID;

/** DTO allégé pour les sélecteurs produit (achat, entrée stock) — sans info de stock ni fournisseur. */
public record ProductSelectorResponse(
        UUID id,
        String nom,
        String reference,
        CategoryProductSummaryResponse category
) {
    public ProductSelectorResponse(Product product) {
        this(
                product.getId(),
                product.getNom(),
                product.getReference(),
                new CategoryProductSummaryResponse(product.getCategoryProduct())
        );
    }
}
