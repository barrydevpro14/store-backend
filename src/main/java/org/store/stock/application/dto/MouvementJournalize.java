package org.store.stock.application.dto;

import org.store.stock.domain.enums.MouvementStockType;

public record MouvementJournalize(
        MouvementStockType type,
        int quantite,
        int stockAvant,
        int stockApres,
        String referenceDocument,
        String commentaire
) {
}
