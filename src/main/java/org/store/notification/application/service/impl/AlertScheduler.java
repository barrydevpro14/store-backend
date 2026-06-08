package org.store.notification.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.achat.domain.repository.FactureAchatRepository;
import org.store.notification.application.event.AbonnementExpiringEvent;
import org.store.notification.application.event.FactureAchatOverdueEvent;
import org.store.notification.application.event.FactureClientOverdueEvent;
import org.store.notification.application.service.INotificationEventPublisher;
import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;
import org.store.notification.domain.service.AlerteDomainService;
import org.store.vente.domain.repository.FactureClientRepository;

import java.time.LocalDate;

/**
 * Daily alert scheduler — runs at 08:00 every morning.
 * Publishes events for:
 *   - Subscriptions expiring in 1, 3 or 5 days
 *   - Unpaid sale invoices overdue by 1, 3 or 5 days
 *   - Unpaid purchase invoices overdue by 1, 3 or 5 days
 */
@Component
public class AlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertScheduler.class);
    private static final int[] ALERT_DAYS = {1, 3, 5};

    private final AbonnementDomainService abonnementDomainService;
    private final FactureClientRepository factureClientRepository;
    private final FactureAchatRepository  factureAchatRepository;
    private final INotificationEventPublisher eventPublisher;
    private final AlerteDomainService alerteDomainService;

    public AlertScheduler(AbonnementDomainService abonnementDomainService,
                          FactureClientRepository factureClientRepository,
                          FactureAchatRepository factureAchatRepository,
                          INotificationEventPublisher eventPublisher,
                          AlerteDomainService alerteDomainService) {
        this.abonnementDomainService = abonnementDomainService;
        this.factureClientRepository = factureClientRepository;
        this.factureAchatRepository  = factureAchatRepository;
        this.eventPublisher          = eventPublisher;
        this.alerteDomainService     = alerteDomainService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void runDailyAlerts() {
        log.info("AlertScheduler: running daily alert checks at 08:00");
        checkAbonnementsExpiring();
        checkFacturesClientOverdue();
        checkFacturesAchatOverdue();
    }

    private void checkAbonnementsExpiring() {
        LocalDate today = LocalDate.now();
        for (int days : ALERT_DAYS) {
            LocalDate targetDate = today.plusDays(days);
            abonnementDomainService.findExpiringOn(targetDate).forEach(abonnement -> {
                String titre = "Abonnement expirant dans " + days + " jour(s)";
                String msg   = "Votre abonnement expire le " + abonnement.getDateFin() + ".";
                alerteDomainService.create(AlerteType.ABONNEMENT_EXPIRING, AlerteStatut.NOUVELLE,
                        titre, msg,
                        abonnement.getEntreprise().getId(), null, abonnement.getId(), days);
                eventPublisher.publishEvent(new AbonnementExpiringEvent(abonnement, days));
                log.info("AbonnementExpiring alert persisted: abonnement {} expires in {} days", abonnement.getId(), days);
            });
        }
    }

    private void checkFacturesClientOverdue() {
        LocalDate today = LocalDate.now();
        for (int days : ALERT_DAYS) {
            LocalDate overdueDate = today.minusDays(days);
            factureClientRepository.findOverdueByDueDate(overdueDate).forEach(facture -> {
                var magasin = facture.getCommande().getMagasin();
                String titre = "Facture vente impayée — " + facture.getNumero() + " (" + days + " j de retard)";
                String msg   = "La facture " + facture.getNumero() + " est impayée depuis " + days + " jour(s).";
                alerteDomainService.create(AlerteType.FACTURE_VENTE_OVERDUE, AlerteStatut.NOUVELLE,
                        titre, msg,
                        magasin.getEntreprise().getId(), magasin.getId(), facture.getId(), days);
                eventPublisher.publishEvent(new FactureClientOverdueEvent(facture, days));
                log.info("FactureClientOverdue alert persisted: facture {} overdue by {} days", facture.getNumero(), days);
            });
        }
    }

    private void checkFacturesAchatOverdue() {
        LocalDate today = LocalDate.now();
        for (int days : ALERT_DAYS) {
            LocalDate overdueDate = today.minusDays(days);
            factureAchatRepository.findOverdueByDueDate(overdueDate).forEach(facture -> {
                var magasin = facture.getCommande().getMagasin();
                String titre = "Facture achat impayée — " + facture.getNumero() + " (" + days + " j de retard)";
                String msg   = "La facture fournisseur " + facture.getNumero() + " est impayée depuis " + days + " jour(s).";
                alerteDomainService.create(AlerteType.FACTURE_ACHAT_OVERDUE, AlerteStatut.NOUVELLE,
                        titre, msg,
                        magasin.getEntreprise().getId(), magasin.getId(), facture.getId(), days);
                eventPublisher.publishEvent(new FactureAchatOverdueEvent(facture, days));
                log.info("FactureAchatOverdue alert persisted: facture {} overdue by {} days", facture.getNumero(), days);
            });
        }
    }
}
