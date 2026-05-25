package org.store.notification.application.event;

import org.store.abonnement.domain.model.PaiementAbonnement;

/** Fired when an owner submits a subscription payment proof — ADMIN must validate or reject. */
public record PaiementAbonnementSubmittedEvent(PaiementAbonnement paiement) {}
