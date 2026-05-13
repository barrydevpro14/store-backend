package org.store.produit.application.dto;

import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFournisseurResponse(
        UUID id,
        ProductSummaryResponse product,
        FournisseurSummaryResponse fournisseur,
        BigDecimal prixAchat,
        String referenceFournisseur,
        String origine
) {
    public ProductFournisseurResponse(ProductFournisseur productFournisseur) {
        this(
                productFournisseur.getId(),
                new ProductSummaryResponse(productFournisseur.getProduct()),
                new FournisseurSummaryResponse(productFournisseur.getFournisseur()),
                productFournisseur.getPrixAchat(),
                productFournisseur.getReferenceFournisseur(),
                productFournisseur.getOrigine()
        );
    }
}
