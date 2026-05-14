package org.store.stock.application.dto;

import org.store.common.tools.DateHelper;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.stock.domain.model.Stock;

import java.math.BigDecimal;
import java.util.UUID;

public record StockResponse(
        UUID id,
        MagasinSummaryResponse magasin,
        ProductSummaryResponse produit,
        int quantiteDisponible,
        int seuilApprovisionnement,
        BigDecimal prixAchatMoyen,
        String createdAt,
        String updatedAt
) {
    public StockResponse(Stock stock) {
        this(
                stock.getId(),
                new MagasinSummaryResponse(stock.getMagasin()),
                new ProductSummaryResponse(stock.getProduit()),
                stock.getQuantiteDisponible(),
                stock.getSeuilApprovisionnement(),
                stock.getPrixAchatMoyen(),
                DateHelper.format(stock.getCreatedAt()),
                DateHelper.format(stock.getUpdatedAt())
        );
    }
}
