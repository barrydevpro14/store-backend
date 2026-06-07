package org.store.notification.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.store.common.i18n.IMessageSourceService;
import org.store.notification.application.event.AbonnementExpiringEvent;
import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;
import org.store.notification.domain.service.AlerteDomainService;
import org.store.notification.application.event.ContactMessageReceivedEvent;
import org.store.notification.application.event.FactureAchatOverdueEvent;
import org.store.notification.application.event.FactureClientOverdueEvent;
import org.store.notification.application.event.PaiementAbonnementRejectedEvent;
import org.store.notification.application.event.PaiementAbonnementSubmittedEvent;
import org.store.notification.application.event.PaiementAbonnementValidatedEvent;
import org.store.notification.application.event.StockBelowThresholdEvent;
import org.store.notification.application.event.VenteValidatedEvent;
import org.store.notification.domain.enums.CanalNotification;
import org.store.notification.domain.enums.NotificationStatut;
import org.store.notification.domain.model.Notification;
import org.store.notification.domain.service.NotificationDomainService;
import org.store.contact.domain.model.ContactMessage;
import org.store.security.domain.model.Account;
import org.store.security.domain.service.AccountDomainService;
import org.store.users.domain.service.EmployeDomainService;
import org.store.users.domain.service.ProprietaireDomainService;

import java.time.LocalDateTime;

/**
 * Listens to business domain events and persists IN_APP Notification rows asynchronously.
 * All notification titles and messages are resolved via IMessageSourceService — no hardcoded text.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationDomainService notificationDomainService;
    private final AlerteDomainService alerteDomainService;
    private final EmployeDomainService employeDomainService;
    private final ProprietaireDomainService proprietaireDomainService;
    private final AccountDomainService accountDomainService;
    private final IMessageSourceService messageSourceService;

    public NotificationEventListener(NotificationDomainService notificationDomainService,
                                     AlerteDomainService alerteDomainService,
                                     EmployeDomainService employeDomainService,
                                     ProprietaireDomainService proprietaireDomainService,
                                     AccountDomainService accountDomainService,
                                     IMessageSourceService messageSourceService) {
        this.notificationDomainService = notificationDomainService;
        this.alerteDomainService = alerteDomainService;
        this.employeDomainService = employeDomainService;
        this.proprietaireDomainService = proprietaireDomainService;
        this.accountDomainService = accountDomainService;
        this.messageSourceService = messageSourceService;
    }

    @Async
    @EventListener
    public void onVenteValidated(VenteValidatedEvent event) {
        var commande = event.commande();
        String titre   = messageSourceService.getMessage("notification.vente.validated.titre", new Object[]{commande.getReference()});
        String message = messageSourceService.getMessage("notification.vente.validated.message", new Object[]{commande.getReference()});

        employeDomainService
                .findActiveAccountsByMagasinIdAndRoleLibelle(commande.getMagasin().getId(), "MANAGER")
                .forEach(account -> createInApp(account, new NotificationPayload(titre, message, null)));

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

        alerteDomainService.create(AlerteType.STOCK_BELOW_THRESHOLD, AlerteStatut.NOUVELLE,
                titre, message,
                stock.getMagasin().getEntreprise().getId(), stock.getMagasin().getId(),
                stock.getProductFournisseur().getId(), null);

        employeDomainService
                .findActiveAccountsByMagasinIdAndRoleLibelle(stock.getMagasin().getId(), "MANAGER")
                .forEach(account -> createInApp(account, new NotificationPayload(titre, message, null)));

        log.info("StockBelowThreshold notification sent for product {}", nom);
    }

    @Async
    @EventListener
    public void onPaiementSubmitted(PaiementAbonnementSubmittedEvent event) {
        var paiement   = event.paiement();
        String sigle   = paiement.getAbonnement().getEntreprise().getSigle();
        String titre   = messageSourceService.getMessage("notification.paiement.submitted.titre", new Object[]{sigle});
        String message = messageSourceService.getMessage("notification.paiement.submitted.message",
                new Object[]{sigle, paiement.getMontantFinal()});

        accountDomainService
                .findAllByRoleLibelle("ADMIN", Pageable.ofSize(100))
                .getContent()
                .forEach(account -> createInApp(account, new NotificationPayload(titre, message, null)));

        log.info("PaiementSubmitted notification sent to ADMINs for paiement {}", paiement.getId());
    }

    @Async
    @EventListener
    public void onPaiementValidated(PaiementAbonnementValidatedEvent event) {
        var paiement   = event.paiement();
        String titre   = messageSourceService.getMessage("notification.paiement.validated.titre");
        String message = messageSourceService.getMessage("notification.paiement.validated.message",
                new Object[]{paiement.getMontantFinal()});

        proprietaireDomainService
                .findAccountByEntrepriseId(paiement.getAbonnement().getEntreprise().getId())
                .ifPresent(account -> createInApp(account, new NotificationPayload(titre, message, null)));

        log.info("PaiementValidated notification sent for paiement {}", paiement.getId());
    }

    @Async
    @EventListener
    public void onPaiementRejected(PaiementAbonnementRejectedEvent event) {
        var paiement   = event.paiement();
        String titre   = messageSourceService.getMessage("notification.paiement.rejected.titre");
        String message = messageSourceService.getMessage("notification.paiement.rejected.message",
                new Object[]{paiement.getMotifRejet()});

        proprietaireDomainService
                .findAccountByEntrepriseId(paiement.getAbonnement().getEntreprise().getId())
                .ifPresent(account -> createInApp(account, new NotificationPayload(titre, message, null)));

        log.info("PaiementRejected notification sent for paiement {}", paiement.getId());
    }

    @Async
    @EventListener
    public void onContactMessageReceived(ContactMessageReceivedEvent event) {
        var contact  = event.contactMessage();
        String titre = messageSourceService.getMessage("notification.contact.received.titre", new Object[]{contact.getSujet()});
        String body  = messageSourceService.getMessage("notification.contact.received.message",
                new Object[]{contact.getNom(), contact.getEmail(), contact.getMessage()});

        accountDomainService
                .findAllByRoleLibelle("ADMIN", Pageable.ofSize(100))
                .getContent()
                .forEach(account -> createInApp(account, new NotificationPayload(titre, body, contact)));

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

        proprietaireDomainService
                .findAccountByEntrepriseId(abonnement.getEntreprise().getId())
                .ifPresent(account -> createInApp(account, new NotificationPayload(titre, message, null)));

        log.info("AbonnementExpiring notification sent: {} days left for abonnement {}", event.joursRestants(), abonnement.getId());
    }

    @Async
    @EventListener
    public void onFactureClientOverdue(FactureClientOverdueEvent event) {
        var facture  = event.facture();
        String titre = messageSourceService.getMessage("notification.facture.vente.overdue.titre",
                new Object[]{facture.getNumero(), event.joursRetard()});
        String msg   = messageSourceService.getMessage("notification.facture.vente.overdue.message",
                new Object[]{facture.getNumero(), event.joursRetard(), facture.getMontantTotal().subtract(facture.getMontantPaye())});

        employeDomainService
                .findActiveAccountsByMagasinIdAndRoleLibelle(facture.getCommande().getMagasin().getId(), "MANAGER")
                .forEach(account -> createInApp(account, new NotificationPayload(titre, msg, null)));

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

        employeDomainService
                .findActiveAccountsByMagasinIdAndRoleLibelle(facture.getCommande().getMagasin().getId(), "MANAGER")
                .forEach(account -> createInApp(account, new NotificationPayload(titre, msg, null)));

        log.info("FactureAchatOverdue notification sent: facture {} overdue by {} days", facture.getNumero(), event.joursRetard());
    }

    private void createInApp(Account destinataire, NotificationPayload payload) {
        Notification notification = new Notification();
        notification.setDestinataire(destinataire);
        notification.setTitre(payload.titre());
        notification.setMessage(payload.message());
        notification.setContact(payload.contact());
        notification.setCanal(CanalNotification.IN_APP);
        notification.setStatut(NotificationStatut.ENVOYEE);
        notification.setDateEnvoi(LocalDateTime.now());
        notificationDomainService.save(notification);
    }

    private record NotificationPayload(String titre, String message, ContactMessage contact) {}
}
