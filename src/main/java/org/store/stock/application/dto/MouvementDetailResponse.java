package org.store.stock.application.dto;

import org.store.stock.domain.enums.MouvementStockType;

public record MouvementDetailResponse(
        MouvementStockType type,
        int quantite,
        int stockAvant,
        int stockApres,
        String referenceDocument,
        String commentaire
) {
}
