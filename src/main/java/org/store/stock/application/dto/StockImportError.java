package org.store.stock.application.dto;

public record StockImportError(
        String referenceProduit,
        String nomProduit,
        String message
) {
}
