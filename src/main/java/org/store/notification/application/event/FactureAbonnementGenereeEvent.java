package org.store.notification.application.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Fired by FacturationAbonnementScheduler when a monthly invoice is auto-generated for an active subscription. */
public record FactureAbonnementGenereeEvent(UUID abonnementId,
                                             UUID entrepriseId,
                                             BigDecimal montantFinal,
                                             LocalDate dateEcheance) {}
