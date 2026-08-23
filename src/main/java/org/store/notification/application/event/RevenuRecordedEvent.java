package org.store.notification.application.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fired once a subscription payment reaches VALIDE. Carries only primitive ids —
 * never the Entreprise entity — because this event is consumed by an @Async
 * listener where the originating Hibernate session/lazy proxies are gone
 * (same class of bug already fixed once on PaiementAbonnementValidatedEvent).
 */
public record RevenuRecordedEvent(UUID entrepriseId, UUID countryId, LocalDate datePaiement, BigDecimal montant) {}
