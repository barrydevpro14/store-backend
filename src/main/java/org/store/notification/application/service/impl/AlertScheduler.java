package org.store.notification.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.repository.FactureAchatRepository;
import org.store.common.i18n.IMessageSourceService;
import org.store.notification.application.event.AbonnementExpiringEvent;
import org.store.notification.application.event.FactureAchatOverdueEvent;
import org.store.notification.application.event.FactureClientOverdueEvent;
import org.store.notification.application.service.INotificationEventPublisher;
import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;
import org.store.notification.domain.service.AlerteDomainService;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.repository.FactureClientRepository;

import java.time.LocalDate;
import java.util.Locale;

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

    private final AbonnementDomainService abonnementDomainService;
    private final FactureClientRepository factureClientRepository;
    private final FactureAchatRepository  factureAchatRepository;
    private final INotificationEventPublisher eventPublisher;
    private final AlerteDomainService alerteDomainService;
    private final IMessageSourceService messageSourceService;

    public AlertScheduler(AbonnementDomainService abonnementDomainService,
                          FactureClientRepository factureClientRepository,
                          FactureAchatRepository factureAchatRepository,
                          INotificationEventPublisher eventPublisher,
                          AlerteDomainService alerteDomainService,
                          IMessageSourceService messageSourceService) {
        this.abonnementDomainService = abonnementDomainService;
        this.factureClientRepository = factureClientRepository;
        this.factureAchatRepository  = factureAchatRepository;
        this.eventPublisher          = eventPublisher;
        this.alerteDomainService     = alerteDomainService;
        this.messageSourceService    = messageSourceService;
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
        java.util.List<java.time.LocalDate> alertDates = java.util.List.of(
                today.plusDays(1), today.plusDays(3), today.plusDays(5));
        abonnementDomainService.findExpiringOnDates(alertDates).forEach(abonnement -> {
            int daysUntil = (int) java.time.temporal.ChronoUnit.DAYS.between(today, abonnement.getDateFin());
            alertAbonnementExpiring(abonnement, daysUntil);
        });
    }

    private void alertAbonnementExpiring(Abonnement abonnement, int daysUntilExpiry) {
        String titre = messageSourceService.getMessage("notification.abonnement.expiring.titre",
                new Object[]{daysUntilExpiry}, Locale.FRENCH);
        String msg   = messageSourceService.getMessage("notification.abonnement.expiring.message",
                new Object[]{daysUntilExpiry, abonnement.getDateFin()}, Locale.FRENCH);
        alerteDomainService.create(AlerteType.ABONNEMENT_EXPIRING, AlerteStatut.NOUVELLE,
                titre, msg,
                abonnement.getEntreprise().getId(), null, abonnement.getId(), daysUntilExpiry);
        eventPublisher.publishEvent(new AbonnementExpiringEvent(abonnement, daysUntilExpiry));
        log.info("AbonnementExpiring alert persisted: abonnement {} expires in {} days", abonnement.getId(), daysUntilExpiry);
    }

    private void checkFacturesClientOverdue() {
        LocalDate today = LocalDate.now();
        java.util.List<java.time.LocalDate> alertDates = java.util.List.of(
                today.plusDays(1), today.plusDays(3), today.plusDays(5));
        factureClientRepository.findDueOnDates(alertDates).forEach(facture -> {
            int daysUntil = (int) java.time.temporal.ChronoUnit.DAYS.between(today, facture.getDateEcheance());
            alertFactureClientOverdue(facture, daysUntil);
        });
    }

    private void alertFactureClientOverdue(FactureClient facture, int daysUntil) {
        var magasin = facture.getCommande().getMagasin();
        String titre = messageSourceService.getMessage("notification.facture.vente.overdue.titre",
                new Object[]{facture.getNumero(), daysUntil}, Locale.FRENCH);
        String msg   = messageSourceService.getMessage("notification.facture.vente.overdue.message",
                new Object[]{facture.getNumero(), daysUntil, facture.getMontantTotal().subtract(facture.getMontantPaye())}, Locale.FRENCH);
        alerteDomainService.create(AlerteType.FACTURE_VENTE_OVERDUE, AlerteStatut.NOUVELLE,
                titre, msg,
                magasin.getEntreprise().getId(), magasin.getId(), facture.getId(), daysUntil);
        eventPublisher.publishEvent(new FactureClientOverdueEvent(facture, daysUntil));
        log.info("FactureClientDue alert persisted: facture {} due in {} days", facture.getNumero(), daysUntil);
    }

    private void checkFacturesAchatOverdue() {
        LocalDate today = LocalDate.now();
        java.util.List<java.time.LocalDate> alertDates = java.util.List.of(
                today.plusDays(1), today.plusDays(3), today.plusDays(5));
        factureAchatRepository.findDueOnDates(alertDates).forEach(facture -> {
            int daysUntil = (int) java.time.temporal.ChronoUnit.DAYS.between(today, facture.getDateEcheance());
            alertFactureAchatOverdue(facture, daysUntil);
        });
    }

    private void alertFactureAchatOverdue(FactureAchat facture, int daysUntil) {
        var magasin = facture.getCommande().getMagasin();
        String titre = messageSourceService.getMessage("notification.facture.achat.overdue.titre",
                new Object[]{facture.getNumero(), daysUntil}, Locale.FRENCH);
        String msg   = messageSourceService.getMessage("notification.facture.achat.overdue.message",
                new Object[]{facture.getNumero(), daysUntil, facture.getMontantTotal().subtract(facture.getMontantPaye())}, Locale.FRENCH);
        alerteDomainService.create(AlerteType.FACTURE_ACHAT_OVERDUE, AlerteStatut.NOUVELLE,
                titre, msg,
                magasin.getEntreprise().getId(), magasin.getId(), facture.getId(), daysUntil);
        eventPublisher.publishEvent(new FactureAchatOverdueEvent(facture, daysUntil));
        log.info("FactureAchatDue alert persisted: facture {} due in {} days", facture.getNumero(), daysUntil);
    }
}
