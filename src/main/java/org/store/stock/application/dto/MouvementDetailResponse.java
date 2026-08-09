package org.store.stock.application.dto;

import org.store.stock.domain.enums.MouvementStockType;

import java.math.BigDecimal;

public record MouvementDetailResponse(
        MouvementStockType type,
        BigDecimal quantite,
        BigDecimal stockAvant,
        BigDecimal stockApres,
        String referenceDocument,
        String commentaire
) {
}
