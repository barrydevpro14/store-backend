package org.store.notification.application.event;

import java.util.UUID;

/** Fired when an admin rejects a subscription payment. */
public record PaiementAbonnementRejectedEvent(UUID paiementId, UUID entrepriseId, String motifRejet) {}
