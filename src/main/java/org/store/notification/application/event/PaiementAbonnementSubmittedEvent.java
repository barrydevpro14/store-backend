package org.store.notification.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Fired when an owner submits a subscription payment proof — ADMIN must validate or reject. */
public record PaiementAbonnementSubmittedEvent(UUID paiementId, String entrepriseSigle, BigDecimal montantFinal) {}
