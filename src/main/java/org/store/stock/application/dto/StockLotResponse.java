package org.store.stock.application.dto;

import org.store.stock.domain.model.EntreeStock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record StockLotResponse(
        UUID id,
        String numeroLot,
        String dateExpiration,
        int quantiteRestante,
        BigDecimal prixAchat,
        BigDecimal prixVente
) {
    public StockLotResponse(EntreeStock lot) {
        this(
                lot.getId(),
                lot.getNumeroLot(),
                lot.getDateExpiration() != null ? lot.getDateExpiration().toString() : null,
                lot.getQuantiteRestante(),
                lot.getPrixAchat() != null ? lot.getPrixAchat().setScale(2, RoundingMode.HALF_UP) : null,
                lot.getProductFournisseur() != null && lot.getProductFournisseur().getPrixVente() != null
                        ? lot.getProductFournisseur().getPrixVente().setScale(2, RoundingMode.HALF_UP)
                        : null
        );
    }
}
