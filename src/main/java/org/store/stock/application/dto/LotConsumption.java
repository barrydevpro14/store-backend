package org.store.stock.application.dto;

public record LotConsumption(
        SortieStockResponse sortie,
        int restantApres
) {
}
