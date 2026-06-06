package org.store.stock.application.dto;

import java.math.BigDecimal;

public record MarginReportResponse(
        BigDecimal margeTotale,
        long quantiteVendueTotale,
        long nombreSorties
) {
    public MarginReportResponse(BigDecimal margeTotale, Long quantiteVendue, Long nombreSorties) {
        this(
                margeTotale != null ? margeTotale : BigDecimal.ZERO,
                quantiteVendue != null ? quantiteVendue : 0L,
                nombreSorties != null ? nombreSorties : 0L
        );
    }
}
