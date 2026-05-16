package org.store.inventaire.application.dto;

import org.store.inventaire.domain.model.LigneInventaire;
import org.store.produit.application.dto.ProductSummaryResponse;

import java.util.UUID;

public record LigneInventaireResponse(
        UUID id,
        UUID inventaireId,
        UUID productFournisseurId,
        ProductSummaryResponse product,
        int quantiteTheorique,
        int quantiteReelle,
        int ecart
) {
    public LigneInventaireResponse(LigneInventaire ligne) {
        this(
                ligne.getId(),
                ligne.getInventaire().getId(),
                ligne.getProductFournisseur().getId(),
                new ProductSummaryResponse(ligne.getProductFournisseur().getProduct()),
                ligne.getQuantiteTheorique(),
                ligne.getQuantiteReelle(),
                ligne.getEcart()
        );
    }
}
