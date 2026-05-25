package org.store.notification.application.event;

import org.store.abonnement.domain.model.PaiementAbonnement;

/** Fired when an admin rejects a subscription payment. */
public record PaiementAbonnementRejectedEvent(PaiementAbonnement paiement) {}
