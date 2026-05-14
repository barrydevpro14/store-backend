package org.store.stock.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StockValuationResponse(
        UUID magasinId,
        BigDecimal valeurTotale,
        long nombreLignes
) {
    public StockValuationResponse(UUID magasinId, BigDecimal valeurTotale, Long nombreLignes) {
        this(magasinId, valeurTotale != null ? valeurTotale : BigDecimal.ZERO, nombreLignes != null ? nombreLignes : 0L);
    }
}
