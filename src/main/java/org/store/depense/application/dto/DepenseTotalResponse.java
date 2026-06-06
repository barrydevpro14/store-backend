package org.store.depense.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DepenseTotalResponse(
        UUID magasinId,
        BigDecimal montantTotal,
        long nombreDepenses
) {
    public DepenseTotalResponse(UUID magasinId, BigDecimal montantTotal, Long nombreDepenses) {
        this(
                magasinId,
                montantTotal != null ? montantTotal : BigDecimal.ZERO,
                nombreDepenses != null ? nombreDepenses : 0L
        );
    }
}
