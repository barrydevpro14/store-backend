package org.store.vente.application.dto;

import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.produit.application.dto.QualitySummaryResponse;
import org.store.vente.domain.enums.LivraisonStatut;
import org.store.vente.domain.model.LigneCommandeVente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LigneCommandeVenteResponse(
        UUID id,
        ProductSummaryResponse product,
        QualitySummaryResponse quality,
        BigDecimal quantite,
        BigDecimal quantiteLivree,
        LivraisonStatut livraisonStatut,
        BigDecimal prixUnitaire,
        BigDecimal montantTotal,
        LocalDate dateAjout
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
                ligne.getMontantTotal(),
                ligne.getDateAjout()
        );
    }
}
