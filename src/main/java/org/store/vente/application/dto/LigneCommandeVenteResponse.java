package org.store.vente.application.dto;

import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.produit.application.dto.QualitySummaryResponse;
import org.store.vente.domain.model.LigneCommandeVente;

import java.math.BigDecimal;
import java.util.UUID;

public record LigneCommandeVenteResponse(
        UUID id,
        ProductSummaryResponse product,
        FournisseurSummaryResponse fournisseur,
        QualitySummaryResponse quality,
        int quantite,
        BigDecimal prixUnitaire,
        BigDecimal montantTotal
) {
    public LigneCommandeVenteResponse(LigneCommandeVente ligne) {
        this(
                ligne.getId(),
                new ProductSummaryResponse(ligne.getProductFournisseur().getProduct()),
                new FournisseurSummaryResponse(ligne.getProductFournisseur().getFournisseur()),
                new QualitySummaryResponse(ligne.getProductFournisseur().getQuality()),
                ligne.getQuantite(),
                ligne.getPrixUnitaire(),
                ligne.getMontantTotal()
        );
    }
}
