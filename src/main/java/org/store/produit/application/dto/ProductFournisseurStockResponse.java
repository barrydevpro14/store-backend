package org.store.produit.application.dto;

import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFournisseurStockResponse(
        UUID id,
        QualitySummaryResponse quality,
        FournisseurSummaryResponse fournisseur,
        BigDecimal prixVente,
        Integer quantiteEnStock
) {
    public ProductFournisseurStockResponse(ProductFournisseur productFournisseur, Integer quantiteEnStock) {
        this(
                productFournisseur.getId(),
                new QualitySummaryResponse(productFournisseur.getQuality()),
                new FournisseurSummaryResponse(productFournisseur.getFournisseur()),
                productFournisseur.getPrixVente(),
                quantiteEnStock
        );
    }
}
