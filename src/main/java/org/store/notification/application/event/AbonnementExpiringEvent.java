package org.store.notification.application.event;

import org.store.abonnement.domain.model.Abonnement;

/** Fired daily at 8h when an active/trial subscription expires in 1, 3 or 5 days. */
public record AbonnementExpiringEvent(Abonnement abonnement, int joursRestants) {}
