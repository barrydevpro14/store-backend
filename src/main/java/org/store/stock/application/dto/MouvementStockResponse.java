package org.store.stock.application.dto;

import org.store.common.tools.DateHelper;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.stock.domain.model.MouvementStock;

import java.util.UUID;

public record MouvementStockResponse(
        UUID id,
        UUID stockId,
        MagasinSummaryResponse magasin,
        ProductSummaryResponse produit,
        MouvementDetailResponse detail,
        String createdAt,
        String createdBy
) {
    public MouvementStockResponse(MouvementStock mouvement) {
        this(
                mouvement.getId(),
                mouvement.getStock().getId(),
                new MagasinSummaryResponse(mouvement.getStock().getMagasin()),
                new ProductSummaryResponse(mouvement.getStock().getProduit()),
                new MouvementDetailResponse(
                        mouvement.getType(),
                        mouvement.getQuantite(),
                        mouvement.getStockAvant(),
                        mouvement.getStockApres(),
                        mouvement.getReferenceDocument(),
                        mouvement.getCommentaire()
                ),
                DateHelper.format(mouvement.getCreatedAt()),
                mouvement.getCreatedBy()
        );
    }
}
