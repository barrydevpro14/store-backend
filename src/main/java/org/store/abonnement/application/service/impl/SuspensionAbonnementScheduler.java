package org.store.abonnement.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.notification.application.event.AbonnementSuspenduEvent;
import org.store.property.SubscriptionProperties;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Daily scheduler: suspends subscriptions whose invoice (FACTURE_GENEREE or EN_ATTENTE_VALIDATION)
 * has passed its dateEcheance + joursSuspension grace period without being validated.
 * Marks the invoice EN_RETARD and the subscription SUSPENDU, then publishes AbonnementSuspenduEvent.
 */
@Component
public class SuspensionAbonnementScheduler {

    private static final Logger log = LoggerFactory.getLogger(SuspensionAbonnementScheduler.class);

    private final IPaiementAbonnementService paiementAbonnementService;
    private final IAbonnementService abonnementService;
    private final ApplicationEventPublisher eventPublisher;
    private final SubscriptionProperties subscriptionProperties;

    public SuspensionAbonnementScheduler(IPaiementAbonnementService paiementAbonnementService,
                                         IAbonnementService abonnementService,
                                         ApplicationEventPublisher eventPublisher,
                                         SubscriptionProperties subscriptionProperties) {
        this.paiementAbonnementService = paiementAbonnementService;
        this.abonnementService = abonnementService;
        this.eventPublisher = eventPublisher;
        this.subscriptionProperties = subscriptionProperties;
    }

    @Scheduled(cron = "${cron.suspension.abonnement}")
    @Transactional
    public void suspendrePourNonPaiement() {
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = today.minusDays(subscriptionProperties.joursSuspension());
        List<PaiementAbonnement> overdueInvoices = paiementAbonnementService.findOverdueInvoices(cutoffDate);

        log.info("SuspensionAbonnementScheduler: {} overdue invoice(s) to process for {} (cutoff={})",
                overdueInvoices.size(), today, cutoffDate);

        overdueInvoices.forEach(facture -> suspendre(facture, today));
    }

    /** Marks the invoice EN_RETARD, suspends the subscription, and publishes the suspension event. */
    private void suspendre(PaiementAbonnement facture, LocalDate today) {
        paiementAbonnementService.markAsEnRetard(facture);

        Abonnement abonnement = facture.getAbonnement();
        abonnementService.suspend(abonnement);

        UUID abonnementId = abonnement.getId();
        LocalDate dateEcheance = facture.getDateEcheance();

        eventPublisher.publishEvent(new AbonnementSuspenduEvent(
                abonnementId,
                abonnement.getEntreprise().getId(),
                dateEcheance));

        log.info("SuspensionAbonnementScheduler: abonnement {} suspended — deadline={}", abonnementId, dateEcheance);
    }
}
