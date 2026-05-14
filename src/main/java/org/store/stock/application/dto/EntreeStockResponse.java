package org.store.stock.application.dto;

import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.stock.domain.model.EntreeStock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EntreeStockResponse(
        UUID id,
        MagasinSummaryResponse magasin,
        ProductSummaryResponse produit,
        FournisseurSummaryResponse fournisseur,
        int quantiteInitiale,
        int quantiteRestante,
        BigDecimal prixAchat,
        String numeroLot,
        LocalDate dateExpiration,
        LocalDateTime createdAt
) {
    public EntreeStockResponse(EntreeStock entreeStock) {
        this(
                entreeStock.getId(),
                new MagasinSummaryResponse(entreeStock.getMagasin()),
                new ProductSummaryResponse(entreeStock.getProduit()),
                entreeStock.getProductFournisseur() != null
                        ? new FournisseurSummaryResponse(entreeStock.getProductFournisseur().getFournisseur())
                        : null,
                entreeStock.getQuantiteInitiale(),
                entreeStock.getQuantiteRestante(),
                entreeStock.getPrixAchat(),
                entreeStock.getNumeroLot(),
                entreeStock.getDateExpiration(),
                entreeStock.getCreatedAt()
        );
    }
}
