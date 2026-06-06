package org.store.stock.application.dto;

import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.common.tools.DateHelper;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.stock.domain.model.EntreeStock;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpiringLotResponse(
        UUID id,
        MagasinSummaryResponse magasin,
        ProductSummaryResponse produit,
        FournisseurSummaryResponse fournisseur,
        String numeroLot,
        String dateExpiration,
        int quantiteRestante,
        BigDecimal prixAchat
) {
    public ExpiringLotResponse(EntreeStock lot) {
        this(
                lot.getId(),
                new MagasinSummaryResponse(lot.getMagasin()),
                new ProductSummaryResponse(lot.getProduit()),
                lot.getProductFournisseur() != null
                        ? new FournisseurSummaryResponse(lot.getProductFournisseur().getFournisseur())
                        : null,
                lot.getNumeroLot(),
                DateHelper.format(lot.getDateExpiration()),
                lot.getQuantiteRestante(),
                lot.getPrixAchat()
        );
    }
}
