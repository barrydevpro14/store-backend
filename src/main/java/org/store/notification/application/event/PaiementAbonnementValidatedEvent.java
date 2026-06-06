package org.store.notification.application.event;

import org.store.abonnement.domain.model.PaiementAbonnement;

/** Fired when an admin validates a subscription payment. */
public record PaiementAbonnementValidatedEvent(PaiementAbonnement paiement) {}
