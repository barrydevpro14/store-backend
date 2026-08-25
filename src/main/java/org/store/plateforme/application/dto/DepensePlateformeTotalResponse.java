package org.store.plateforme.application.dto;

import java.math.BigDecimal;

public record DepensePlateformeTotalResponse(
        BigDecimal montantTotal,
        long nombreDepenses
) {
    public DepensePlateformeTotalResponse(BigDecimal montantTotal, Long nombreDepenses) {
        this(
                montantTotal != null ? montantTotal : BigDecimal.ZERO,
                nombreDepenses != null ? nombreDepenses : 0L
        );
    }
}
