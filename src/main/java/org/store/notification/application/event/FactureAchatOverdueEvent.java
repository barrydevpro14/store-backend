package org.store.notification.application.event;

import org.store.achat.domain.model.FactureAchat;

/** Fired daily at 8h when an unpaid purchase invoice is 1, 3 or 5 days overdue. */
public record FactureAchatOverdueEvent(FactureAchat facture, int joursRetard) {}
