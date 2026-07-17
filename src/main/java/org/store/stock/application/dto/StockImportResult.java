package org.store.stock.application.dto;

import java.util.List;

public record StockImportResult(
        int lignesImportees,
        int lignesIgnorees,
        List<StockImportError> erreurs
) {
}
