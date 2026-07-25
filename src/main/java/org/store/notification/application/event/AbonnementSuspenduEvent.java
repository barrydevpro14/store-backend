package org.store.notification.application.event;

import java.time.LocalDate;
import java.util.UUID;

/** Fired by SuspensionAbonnementScheduler when a subscription is suspended for non-payment. */
public record AbonnementSuspenduEvent(UUID abonnementId,
                                       UUID entrepriseId,
                                       LocalDate dateEcheanceDepassee) {}
