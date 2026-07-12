package org.store.vente.application.dto;

import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.produit.application.dto.QualitySummaryResponse;
import org.store.vente.domain.enums.LivraisonStatut;
import org.store.vente.domain.model.LigneCommandeVente;

import java.math.BigDecimal;
import java.util.UUID;

public record LigneCommandeVenteResponse(
        UUID id,
        ProductSummaryResponse product,
        QualitySummaryResponse quality,
        int quantite,
        int quantiteLivree,
        LivraisonStatut livraisonStatut,
        BigDecimal prixUnitaire,
        BigDecimal montantTotal
) {
    public LigneCommandeVenteResponse(LigneCommandeVente ligne) {
        this(
                ligne.getId(),
                new ProductSummaryResponse(ligne.getProductFournisseur().getProduct()),
                new QualitySummaryResponse(ligne.getProductFournisseur().getQuality()),
                ligne.getQuantite(),
                ligne.getQuantiteLivree(),
                ligne.getLivraisonStatut(),
                ligne.getPrixUnitaire(),
                ligne.getMontantTotal()
        );
    }
}
