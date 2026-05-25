package org.store.notification.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.store.notification.application.event.ContactMessageReceivedEvent;
import org.store.notification.application.event.PaiementAbonnementRejectedEvent;
import org.store.notification.application.event.PaiementAbonnementSubmittedEvent;
import org.store.notification.application.event.PaiementAbonnementValidatedEvent;
import org.store.notification.application.event.StockBelowThresholdEvent;
import org.store.notification.application.event.VenteValidatedEvent;
import org.store.notification.domain.enums.CanalNotification;
import org.store.notification.domain.enums.NotificationStatut;
import org.store.notification.domain.model.Notification;
import org.store.notification.domain.service.NotificationDomainService;
import org.store.security.domain.model.Account;
import org.store.security.domain.service.AccountDomainService;
import org.store.users.domain.service.EmployeDomainService;
import org.store.users.domain.service.ProprietaireDomainService;

import java.time.LocalDateTime;

/**
 * Listens to business domain events and persists IN_APP Notification rows asynchronously.
 * Each handler resolves the target account(s) and delegates persistence to NotificationDomainService.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationDomainService notificationDomainService;
    private final EmployeDomainService employeDomainService;
    private final ProprietaireDomainService proprietaireDomainService;
    private final AccountDomainService accountDomainService;

    public NotificationEventListener(NotificationDomainService notificationDomainService,
                                     EmployeDomainService employeDomainService,
                                     ProprietaireDomainService proprietaireDomainService,
                                     AccountDomainService accountDomainService) {
        this.notificationDomainService = notificationDomainService;
        this.employeDomainService = employeDomainService;
        this.proprietaireDomainService = proprietaireDomainService;
        this.accountDomainService = accountDomainService;
    }

    @Async
    @EventListener
    public void onVenteValidated(VenteValidatedEvent event) {
        var commande = event.commande();
        String titre = "Vente validée — " + commande.getReference();
        String message = "La commande " + commande.getReference() + " a été validée.";
        var magasinId = commande.getMagasin().getId();

        employeDomainService.findActiveAccountsByMagasinIdAndRoleLibelle(magasinId, "MANAGER")
                .forEach(account -> createInApp(account, titre, message));

        log.info("VenteValidated notification sent for commande {}", commande.getReference());
    }

    @Async
    @EventListener
    public void onStockBelowThreshold(StockBelowThresholdEvent event) {
        var stock = event.stock();
        String produitNom = stock.getProduit().getNom();
        String titre = "Stock bas — " + produitNom;
        String message = "Le stock de « " + produitNom + " » est en dessous du seuil d'approvisionnement (" + stock.getQuantiteDisponible() + " unité(s)).";
        var magasinId = stock.getMagasin().getId();

        employeDomainService.findActiveAccountsByMagasinIdAndRoleLibelle(magasinId, "MANAGER")
                .forEach(account -> createInApp(account, titre, message));

        log.info("StockBelowThreshold notification sent for product {}", produitNom);
    }

    @Async
    @EventListener
    public void onPaiementValidated(PaiementAbonnementValidatedEvent event) {
        var paiement = event.paiement();
        var entrepriseId = paiement.getAbonnement().getEntreprise().getId();
        String titre = "Paiement validé";
        String message = "Votre paiement de " + paiement.getMontantFinal() + " XOF a été validé. Votre abonnement est actif.";

        proprietaireDomainService.findAccountByEntrepriseId(entrepriseId)
                .ifPresent(account -> createInApp(account, titre, message));

        log.info("PaiementValidated notification sent for paiement {}", paiement.getId());
    }

    @Async
    @EventListener
    public void onPaiementRejected(PaiementAbonnementRejectedEvent event) {
        var paiement = event.paiement();
        var entrepriseId = paiement.getAbonnement().getEntreprise().getId();
        String titre = "Paiement rejeté";
        String message = "Votre paiement a été rejeté. Motif : " + paiement.getMotifRejet();

        proprietaireDomainService.findAccountByEntrepriseId(entrepriseId)
                .ifPresent(account -> createInApp(account, titre, message));

        log.info("PaiementRejected notification sent for paiement {}", paiement.getId());
    }

    @Async
    @EventListener
    public void onPaiementSubmitted(PaiementAbonnementSubmittedEvent event) {
        var paiement = event.paiement();
        var entrepriseSigle = paiement.getAbonnement().getEntreprise().getSigle();
        String titre = "Nouveau paiement à valider — " + entrepriseSigle;
        String message = "L'entreprise « " + entrepriseSigle + " » a soumis un paiement de "
                + paiement.getMontantFinal() + " XOF. En attente de validation.";
        accountDomainService.findAllByRoleLibelle("ADMIN", org.springframework.data.domain.Pageable.ofSize(100))
                .getContent()
                .forEach(account -> createInApp(account, titre, message));
        log.info("PaiementSubmitted notification sent to ADMINs for paiement {}", paiement.getId());
    }

    @Async
    @EventListener
    public void onContactMessageReceived(ContactMessageReceivedEvent event) {
        var contactMessage = event.contactMessage();
        String titre = "Contact : " + contactMessage.getSujet();
        String body = contactMessage.getNom() + " <" + contactMessage.getEmail() + ">\n" + contactMessage.getMessage();

        accountDomainService.findAllByRoleLibelle("ADMIN", org.springframework.data.domain.Pageable.ofSize(100))
                .getContent()
                .forEach(account -> createInApp(account, titre, body));

        log.info("ContactMessageReceived notification sent for contact from {}", contactMessage.getEmail());
    }

    private void createInApp(Account destinataire, String titre, String message) {
        Notification notification = new Notification();
        notification.setDestinataire(destinataire);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setCanal(CanalNotification.IN_APP);
        notification.setStatut(NotificationStatut.ENVOYEE);
        notification.setDateEnvoi(LocalDateTime.now());
        notificationDomainService.save(notification);
    }
}
