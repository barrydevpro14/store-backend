package org.store.stock.application.dto;

import java.math.BigDecimal;

public record LotConsumption(
        SortieStockResponse sortie,
        BigDecimal restantApres
) {
}
