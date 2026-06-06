package org.store.notification.application.event;

import org.store.stock.domain.model.Stock;

/** Fired when stock quantity falls below seuilApprovisionnement after a decrement. */
public record StockBelowThresholdEvent(Stock stock) {}
