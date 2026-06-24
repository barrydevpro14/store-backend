package org.store.notification.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.domain.model.Abonnement;
import org.store.achat.application.service.IFactureAchatService;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.FactureAchat;
import org.store.common.i18n.IMessageSourceService;
import org.store.notification.application.event.AbonnementExpiringEvent;
import org.store.notification.application.event.FactureAchatOverdueEvent;
import org.store.notification.application.event.FactureClientOverdueEvent;
import org.store.notification.application.service.IAlertService;
import org.store.notification.application.service.INotificationEventPublisher;
import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;
import org.store.notification.domain.service.AlerteDomainService;
import org.store.vente.application.service.IFactureClientService;
import org.store.vente.domain.model.FactureClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Daily alert scheduler — runs at 08:00 every morning.
 * Publishes events for:
 *   - Subscriptions expiring in 0, 1, 3 or 5 days
 *   - Unpaid sale invoices overdue by 0, 1, 3 or 5 days
 *   - Unpaid purchase invoices overdue by 0, 1, 3 or 5 days
 */
@Component
public class AlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertScheduler.class);

    private final IAbonnementService abonnementService;
    private final IFactureClientService factureClientService;
    private final IFactureAchatService factureAchatService;
    private final INotificationEventPublisher eventPublisher;
    private final IAlertService alertService;
    private final IMessageSourceService messageSourceService;

    public AlertScheduler(IAbonnementService abonnementService, IFactureClientService factureClientService, IFactureAchatService factureAchatService,
                          INotificationEventPublisher eventPublisher,
                           IAlertService alertService,
                          IMessageSourceService messageSourceService) {
        this.abonnementService = abonnementService;
        this.factureClientService = factureClientService;
        this.factureAchatService = factureAchatService;
        this.eventPublisher          = eventPublisher;
        this.alertService = alertService;
        this.messageSourceService    = messageSourceService;
    }

    @Scheduled(cron = "${alert.daily}")
    public void runDailyAlerts() {
        log.info("AlertScheduler: running daily alert checks at {}", LocalDateTime.now());
        runDailyAlertsAsync();
    }

    @Async
    public void runDailyAlertsAsync(){
//        checkAbonnementsExpiring();
        checkFacturesClientOverdue();
        checkFacturesAchatOverdue();
    }
    private void checkAbonnementsExpiring() {
        LocalDate today = LocalDate.now();
        java.util.List<java.time.LocalDate> alertDates = java.util.List.of(
                today, today.plusDays(1), today.plusDays(3), today.plusDays(5));
        abonnementService.findExpiringOnDates(alertDates).forEach(abonnement -> {
            int daysUntil = (int) java.time.temporal.ChronoUnit.DAYS.between(today, abonnement.getDateFin());
            alertAbonnementExpiring(abonnement, daysUntil);
        });
    }

    private void alertAbonnementExpiring(Abonnement abonnement, int daysUntilExpiry) {
        boolean isToday = daysUntilExpiry == 0;
        String titre = isToday
                ? messageSourceService.getMessage("notification.abonnement.expiring.titre.today", new Object[]{}, Locale.FRENCH)
                : messageSourceService.getMessage("notification.abonnement.expiring.titre", new Object[]{daysUntilExpiry}, Locale.FRENCH);
        String msg = isToday
                ? messageSourceService.getMessage("notification.abonnement.expiring.message.today", new Object[]{abonnement.getDateFin()}, Locale.FRENCH)
                : messageSourceService.getMessage("notification.abonnement.expiring.message", new Object[]{daysUntilExpiry, abonnement.getDateFin()}, Locale.FRENCH);
        alertService.create(AlerteType.ABONNEMENT_EXPIRING, AlerteStatut.NOUVELLE,
                titre, msg,
                abonnement.getEntreprise().getId(), null, abonnement.getId(), daysUntilExpiry);
        eventPublisher.publishEvent(new AbonnementExpiringEvent(abonnement, daysUntilExpiry));
        log.info("AbonnementExpiring alert persisted: abonnement {} expires in {} days", abonnement.getId(), daysUntilExpiry);
    }

    private void checkFacturesClientOverdue() {
        LocalDate today = LocalDate.now();
        java.util.List<java.time.LocalDate> alertDates = java.util.List.of(
                today, today.plusDays(1), today.plusDays(3), today.plusDays(5));
        factureClientService.findDueOnDates(alertDates , List.of(StatutFacture.NON_PAYEE , StatutFacture.PARTIELLEMENT_PAYEE)).forEach(facture -> {
            int daysUntil = (int) java.time.temporal.ChronoUnit.DAYS.between(today, facture.getDateEcheance());
            alertFactureClientOverdue(facture, daysUntil);
        });
    }

    private void alertFactureClientOverdue(FactureClient facture, int daysUntil) {
        var magasin = facture.getCommande().getMagasin();
        var restant = facture.getMontantTotal().subtract(facture.getMontantPaye());
        boolean isToday = daysUntil == 0;
        String titre = isToday
                ? messageSourceService.getMessage("notification.facture.vente.overdue.titre.today", new Object[]{facture.getNumero()}, Locale.FRENCH)
                : messageSourceService.getMessage("notification.facture.vente.overdue.titre", new Object[]{facture.getNumero(), daysUntil}, Locale.FRENCH);
        String msg = isToday
                ? messageSourceService.getMessage("notification.facture.vente.overdue.message.today", new Object[]{facture.getNumero(), restant}, Locale.FRENCH)
                : messageSourceService.getMessage("notification.facture.vente.overdue.message", new Object[]{facture.getNumero(), daysUntil, restant}, Locale.FRENCH);
        alertService.create(AlerteType.FACTURE_VENTE_OVERDUE, AlerteStatut.NOUVELLE,
                titre, msg,
                magasin.getEntreprise().getId(), magasin.getId(), facture.getCommande().getId(), daysUntil);
        eventPublisher.publishEvent(new FactureClientOverdueEvent(facture, daysUntil));
        log.info("FactureClientDue alert persisted: facture {} due in {} days", facture.getNumero(), daysUntil);
    }

    private void checkFacturesAchatOverdue() {
        LocalDate today = LocalDate.now();
        java.util.List<java.time.LocalDate> alertDates = java.util.List.of(
                today, today.plusDays(1), today.plusDays(3), today.plusDays(5));
        factureAchatService.findDueOnDates(alertDates , List.of(StatutFacture.NON_PAYEE , StatutFacture.PARTIELLEMENT_PAYEE)).forEach(facture -> {
            int daysUntil = (int) java.time.temporal.ChronoUnit.DAYS.between(today, facture.getDateEcheance());
            alertFactureAchatOverdue(facture, daysUntil);
        });
    }

    private void alertFactureAchatOverdue(FactureAchat facture, int daysUntil) {
        var magasin = facture.getCommande().getMagasin();
        var restant = facture.getMontantTotal().subtract(facture.getMontantPaye());
        boolean isToday = daysUntil == 0;
        String titre = isToday
                ? messageSourceService.getMessage("notification.facture.achat.overdue.titre.today", new Object[]{facture.getNumero()}, Locale.FRENCH)
                : messageSourceService.getMessage("notification.facture.achat.overdue.titre", new Object[]{facture.getNumero(), daysUntil}, Locale.FRENCH);
        String msg = isToday
                ? messageSourceService.getMessage("notification.facture.achat.overdue.message.today", new Object[]{facture.getNumero(), restant}, Locale.FRENCH)
                : messageSourceService.getMessage("notification.facture.achat.overdue.message", new Object[]{facture.getNumero(), daysUntil, restant}, Locale.FRENCH);
        alertService.create(AlerteType.FACTURE_ACHAT_OVERDUE, AlerteStatut.NOUVELLE,
                titre, msg,
                magasin.getEntreprise().getId(), magasin.getId(), facture.getCommande().getId(), daysUntil);
        eventPublisher.publishEvent(new FactureAchatOverdueEvent(facture, daysUntil));
        log.info("FactureAchatDue alert persisted: facture {} due in {} days", facture.getNumero(), daysUntil);
    }
}
