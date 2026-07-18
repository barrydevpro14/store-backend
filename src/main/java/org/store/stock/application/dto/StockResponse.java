package org.store.stock.application.dto;

import org.store.common.tools.DateHelper;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.produit.application.dto.QualitySummaryResponse;
import org.store.produit.domain.model.Quality;
import org.store.stock.domain.model.Stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record StockResponse(
        UUID id,
        MagasinSummaryResponse magasin,
        ProductSummaryResponse produit,
        QualitySummaryResponse quality,
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
                new ProductSummaryResponse(stock.getProductFournisseur().getProduct()),
                toQuality(stock.getProductFournisseur().getQuality()),
                stock.getQuantiteDisponible(),
                stock.getSeuilApprovisionnement(),
                stock.getPrixAchatMoyen() != null ? stock.getPrixAchatMoyen().setScale(2, RoundingMode.HALF_UP) : null,
                DateHelper.format(stock.getCreatedAt()),
                DateHelper.format(stock.getUpdatedAt())
        );
    }

    private static QualitySummaryResponse toQuality(Quality quality) {
        return quality != null ? new QualitySummaryResponse(quality) : null;
    }
}
