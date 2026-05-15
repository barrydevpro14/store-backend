package org.store.produit.application.dto;

import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFournisseurResponse(
        UUID id,
        ProductSummaryResponse product,
        FournisseurSummaryResponse fournisseur,
        QualitySummaryResponse quality,
        BigDecimal prixAchat,
        BigDecimal prixVente,
        String referenceFournisseur,
        String origine
) {
    public ProductFournisseurResponse(ProductFournisseur productFournisseur) {
        this(
                productFournisseur.getId(),
                new ProductSummaryResponse(productFournisseur.getProduct()),
                new FournisseurSummaryResponse(productFournisseur.getFournisseur()),
                new QualitySummaryResponse(productFournisseur.getQuality()),
                productFournisseur.getPrixAchat(),
                productFournisseur.getPrixVente(),
                productFournisseur.getReferenceFournisseur(),
                productFournisseur.getOrigine()
        );
    }
}
