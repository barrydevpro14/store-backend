package org.store.notification.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.store.common.i18n.IMessageSourceService;
import org.store.notification.application.dto.NotificationPayload;
import org.store.notification.application.event.AbonnementExpiringEvent;
import org.store.notification.application.event.AbonnementSuspenduEvent;
import org.store.notification.application.event.ContactMessageReceivedEvent;
import org.store.notification.application.event.FactureAchatOverdueEvent;
import org.store.notification.application.event.FactureAbonnementGenereeEvent;
import org.store.notification.application.event.FactureClientOverdueEvent;
import org.store.notification.application.event.PaiementAbonnementRejectedEvent;
import org.store.notification.application.event.PaiementAbonnementSubmittedEvent;
import org.store.notification.application.event.PaiementAbonnementValidatedEvent;
import org.store.notification.application.event.StockBelowThresholdEvent;
import org.store.notification.application.event.VenteValidatedEvent;
import org.store.notification.application.service.IAlertService;
import org.store.notification.application.service.INotificationService;
import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;
import org.store.security.application.service.IAccountService;
import org.store.security.application.service.IRefreshTokenService;
import org.store.users.application.service.IEmployeService;

/**
 * Listens to business domain events and persists IN_APP Notification rows asynchronously.
 * All notification titles and messages are resolved via IMessageSourceService — no hardcoded text.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final INotificationService notificationService;
    private final IAlertService alertService;
    private final IEmployeService employeService;
    private final IAccountService accountService;
    private final IMessageSourceService messageSourceService;
    private final IRefreshTokenService refreshTokenService;

    public NotificationEventListener(INotificationService notificationService,
                                     IAlertService alertService,
                                     IEmployeService employeService,
                                     IAccountService accountService,
                                     IMessageSourceService messageSourceService,
                                     IRefreshTokenService refreshTokenService) {
        this.notificationService = notificationService;
        this.alertService = alertService;
        this.employeService = employeService;
        this.accountService = accountService;
        this.messageSourceService = messageSourceService;
        this.refreshTokenService = refreshTokenService;
    }

    @Async
    @EventListener
    public void onVenteValidated(VenteValidatedEvent event) {
        var commande = event.commande();
        String titre   = messageSourceService.getMessage("notification.vente.validated.titre", new Object[]{commande.getReference()});
        String message = messageSourceService.getMessage("notification.vente.validated.message", new Object[]{commande.getReference()});
        NotificationPayload payload = new NotificationPayload(titre, message, null);

        employeService.findActiveAccountsByMagasinIdAndRoleLibelle(commande.getMagasin().getId(), "MANAGER")
                .forEach(account -> notificationService.createInApp(account, payload));

        log.info("VenteValidated notification sent for commande {}", commande.getReference());
    }

    @Async
    @EventListener
    public void onStockBelowThreshold(StockBelowThresholdEvent event) {
        var stock      = event.stock();
        String nom     = stock.getProductFournisseur().getProduct().getNom();
        String titre   = messageSourceService.getMessage("notification.stock.belowThreshold.titre", new Object[]{nom});
        String message = messageSourceService.getMessage("notification.stock.belowThreshold.message",
                new Object[]{nom, stock.getQuantiteDisponible()});
        NotificationPayload payload = new NotificationPayload(titre, message, null);

        alertService.create(AlerteType.STOCK_BELOW_THRESHOLD, AlerteStatut.NOUVELLE,
                titre, message,
                stock.getMagasin().getEntreprise().getId(), stock.getMagasin().getId(),
                stock.getProductFournisseur().getId(), null);

        employeService.findActiveAccountsByMagasinIdAndRoleLibelle(stock.getMagasin().getId(), "MANAGER")
                .forEach(account -> notificationService.createInApp(account, payload));

        log.info("StockBelowThreshold notification sent for product {}", nom);
    }

    @Async
    @EventListener
    public void onPaiementSubmitted(PaiementAbonnementSubmittedEvent event) {
        String sigle   = event.entrepriseSigle();
        String titre   = messageSourceService.getMessage("notification.paiement.submitted.titre", new Object[]{sigle});
        String message = messageSourceService.getMessage("notification.paiement.submitted.message",
                new Object[]{sigle, event.montantFinal()});
        NotificationPayload payload = new NotificationPayload(titre, message, null);

        accountService.findAllByRoleLibelle("ADMIN")
                .forEach(account -> notificationService.createInApp(account, payload));

        log.info("PaiementSubmitted notification sent to ADMINs for paiement {}", event.paiementId());
    }

    @Async
    @EventListener
    public void onPaiementValidated(PaiementAbonnementValidatedEvent event) {
        String titre   = messageSourceService.getMessage("notification.paiement.validated.titre");
        String message = messageSourceService.getMessage("notification.paiement.validated.message",
                new Object[]{event.montantFinal()});

        notificationService.sendInAppToEntreprise(event.entrepriseId(), new NotificationPayload(titre, message, null));

        log.info("PaiementValidated notification sent for paiement {}", event.paiementId());
    }

    @Async
    @EventListener
    public void onPaiementRejected(PaiementAbonnementRejectedEvent event) {
        String titre   = messageSourceService.getMessage("notification.paiement.rejected.titre");
        String message = messageSourceService.getMessage("notification.paiement.rejected.message",
                new Object[]{event.motifRejet()});

        notificationService.sendInAppToEntreprise(event.entrepriseId(), new NotificationPayload(titre, message, null));

        log.info("PaiementRejected notification sent for paiement {}", event.paiementId());
    }

    @Async
    @EventListener
    public void onContactMessageReceived(ContactMessageReceivedEvent event) {
        var contact  = event.contactMessage();
        String titre = messageSourceService.getMessage("notification.contact.received.titre", new Object[]{contact.getSujet()});
        String body  = messageSourceService.getMessage("notification.contact.received.message",
                new Object[]{contact.getNom(), contact.getEmail(), contact.getMessage()});
        NotificationPayload payload = new NotificationPayload(titre, body, contact);

        accountService.findAllByRoleLibelle("ADMIN")
                .forEach(account -> notificationService.createInApp(account, payload));

        log.info("ContactMessageReceived notification sent for contact from {}", contact.getEmail());
    }

    @Async
    @EventListener
    public void onAbonnementExpiring(AbonnementExpiringEvent event) {
        var abonnement = event.abonnement();
        String titre   = messageSourceService.getMessage("notification.abonnement.expiring.titre",
                new Object[]{event.joursRestants()});
        String message = messageSourceService.getMessage("notification.abonnement.expiring.message",
                new Object[]{event.joursRestants(), abonnement.getDateFin()});

        notificationService.sendInAppToEntreprise(abonnement.getEntreprise().getId(),
                new NotificationPayload(titre, message, null));

        log.info("AbonnementExpiring notification sent: {} days left for abonnement {}", event.joursRestants(), abonnement.getId());
    }

    @Async
    @EventListener
    public void onFactureAbonnementGeneree(FactureAbonnementGenereeEvent event) {
        String periode = event.dateEcheance()
                .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.FRENCH));
        String titre   = messageSourceService.getMessage("notification.abonnement.facture.titre");
        String message = messageSourceService.getMessage("notification.abonnement.facture.message",
                new Object[]{periode, event.montantFinal(), event.dateEcheance()});

        notificationService.sendInAppToEntreprise(event.entrepriseId(), new NotificationPayload(titre, message, null));

        log.info("FactureAbonnementGeneree notification sent for entreprise {}", event.entrepriseId());
    }

    @Async
    @EventListener
    public void onAbonnementSuspendu(AbonnementSuspenduEvent event) {
        String titre   = messageSourceService.getMessage("notification.abonnement.suspendu.titre");
        String message = messageSourceService.getMessage("notification.abonnement.suspendu.message",
                new Object[]{event.dateEcheanceDepassee()});

        notificationService.sendInAppToEntreprise(event.entrepriseId(), new NotificationPayload(titre, message, null));

        refreshTokenService.revokeAllByEntrepriseId(event.entrepriseId());

        log.info("AbonnementSuspendu: tokens revoked and notification sent for abonnement {}", event.abonnementId());
    }

    @Async
    @EventListener
    public void onFactureClientOverdue(FactureClientOverdueEvent event) {
        var facture  = event.facture();
        String titre = messageSourceService.getMessage("notification.facture.vente.overdue.titre",
                new Object[]{facture.getNumero(), event.joursRetard()});
        String msg   = messageSourceService.getMessage("notification.facture.vente.overdue.message",
                new Object[]{facture.getNumero(), event.joursRetard(), facture.getMontantTotal().subtract(facture.getMontantPaye())});
        NotificationPayload payload = new NotificationPayload(titre, msg, null);

        employeService.findActiveAccountsByMagasinIdAndRoleLibelle(facture.getCommande().getMagasin().getId(), "MANAGER")
                .forEach(account -> notificationService.createInApp(account, payload));

        log.info("FactureClientOverdue notification sent: facture {} overdue by {} days", facture.getNumero(), event.joursRetard());
    }

    @Async
    @EventListener
    public void onFactureAchatOverdue(FactureAchatOverdueEvent event) {
        var facture  = event.facture();
        String titre = messageSourceService.getMessage("notification.facture.achat.overdue.titre",
                new Object[]{facture.getNumero(), event.joursRetard()});
        String msg   = messageSourceService.getMessage("notification.facture.achat.overdue.message",
                new Object[]{facture.getNumero(), event.joursRetard(), facture.getMontantTotal().subtract(facture.getMontantPaye())});
        NotificationPayload payload = new NotificationPayload(titre, msg, null);

        employeService.findActiveAccountsByMagasinIdAndRoleLibelle(facture.getCommande().getMagasin().getId(), "MANAGER")
                .forEach(account -> notificationService.createInApp(account, payload));

        log.info("FactureAchatOverdue notification sent: facture {} overdue by {} days", facture.getNumero(), event.joursRetard());
    }
}
