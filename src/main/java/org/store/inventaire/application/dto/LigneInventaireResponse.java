package org.store.inventaire.application.dto;

import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.inventaire.domain.model.LigneInventaire;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.produit.application.dto.QualitySummaryResponse;
import org.store.produit.domain.model.Quality;

import java.math.BigDecimal;
import java.util.UUID;

public record LigneInventaireResponse(
        UUID id,
        UUID inventaireId,
        UUID productFournisseurId,
        ProductSummaryResponse product,
        FournisseurSummaryResponse fournisseur,
        QualitySummaryResponse quality,
        int quantiteTheorique,
        int quantiteReelle,
        int ecart,
        BigDecimal prixUnitaire
) {
    public LigneInventaireResponse(LigneInventaire ligne) {
        this(
                ligne.getId(),
                ligne.getInventaire().getId(),
                ligne.getProductFournisseur().getId(),
                new ProductSummaryResponse(ligne.getProductFournisseur().getProduct()),
                new FournisseurSummaryResponse(ligne.getProductFournisseur().getFournisseur()),
                toQualitySummary(ligne.getProductFournisseur().getQuality()),
                ligne.getQuantiteTheorique(),
                ligne.getQuantiteReelle(),
                ligne.getEcart(),
                ligne.getPrixUnitaire() != null
                        ? ligne.getPrixUnitaire()
                        : ligne.getProductFournisseur().getPrixAchat()
        );
    }

    private static QualitySummaryResponse toQualitySummary(Quality quality) {
        return quality != null ? new QualitySummaryResponse(quality) : null;
    }
}
