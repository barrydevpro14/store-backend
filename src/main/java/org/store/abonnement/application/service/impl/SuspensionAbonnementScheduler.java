package org.store.abonnement.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.abonnement.domain.service.PaiementAbonnementDomainService;
import org.store.notification.application.event.AbonnementSuspenduEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Daily scheduler: suspends subscriptions whose invoice (FACTURE_GENEREE or EN_ATTENTE_VALIDATION)
 * has passed its dateEcheance without being validated. Marks the invoice EN_RETARD and the
 * subscription SUSPENDU, then publishes AbonnementSuspenduEvent.
 */
@Component
public class SuspensionAbonnementScheduler {

    private static final Logger log = LoggerFactory.getLogger(SuspensionAbonnementScheduler.class);

    private final PaiementAbonnementDomainService paiementAbonnementDomainService;
    private final AbonnementDomainService abonnementDomainService;
    private final ApplicationEventPublisher eventPublisher;

    public SuspensionAbonnementScheduler(PaiementAbonnementDomainService paiementAbonnementDomainService,
                                         AbonnementDomainService abonnementDomainService,
                                         ApplicationEventPublisher eventPublisher) {
        this.paiementAbonnementDomainService = paiementAbonnementDomainService;
        this.abonnementDomainService = abonnementDomainService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "${cron.suspension.abonnement}")
    @Transactional
    public void suspendrePourNonPaiement() {
        LocalDate today = LocalDate.now();
        List<PaiementAbonnement> overdueInvoices = paiementAbonnementDomainService.findOverdueInvoices(today);

        log.info("SuspensionAbonnementScheduler: {} overdue invoice(s) to process for {}", overdueInvoices.size(), today);

        overdueInvoices.forEach(facture -> suspendre(facture, today));
    }

    /** Marks the invoice EN_RETARD, suspends the subscription, and publishes the suspension event. */
    private void suspendre(PaiementAbonnement facture, LocalDate today) {
        paiementAbonnementDomainService.markAsEnRetard(facture);

        Abonnement abonnement = facture.getAbonnement();
        abonnementDomainService.suspend(abonnement);

        UUID abonnementId = abonnement.getId();
        LocalDate dateEcheance = facture.getDateEcheance();

        eventPublisher.publishEvent(new AbonnementSuspenduEvent(
                abonnementId,
                abonnement.getEntreprise().getId(),
                dateEcheance));

        log.info("SuspensionAbonnementScheduler: abonnement {} suspended — deadline={}", abonnementId, dateEcheance);
    }
}
