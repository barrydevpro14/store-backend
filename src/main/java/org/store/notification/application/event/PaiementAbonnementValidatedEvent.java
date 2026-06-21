package org.store.notification.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Fired when an admin validates a subscription payment. */
public record PaiementAbonnementValidatedEvent(UUID paiementId, UUID entrepriseId, BigDecimal montantFinal) {}
