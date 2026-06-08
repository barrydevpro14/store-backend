package org.store.notification.application.event;

import org.store.vente.domain.model.FactureClient;

/** Fired daily at 8h when an unpaid sale invoice is 1, 3 or 5 days overdue. */
public record FactureClientOverdueEvent(FactureClient facture, int joursRetard) {}
