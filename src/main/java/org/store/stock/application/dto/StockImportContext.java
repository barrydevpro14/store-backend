package org.store.stock.application.dto;

import java.util.List;
import java.util.UUID;

public record StockImportContext(
        List<LigneEntreeStockRequest> validLignes,
        List<StockImportError> errors,
        UUID[] pieceUnitRef
) {}
