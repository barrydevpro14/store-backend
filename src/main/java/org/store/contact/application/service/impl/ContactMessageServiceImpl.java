package org.store.contact.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.contact.application.dto.ContactMessageRequest;
import org.store.contact.application.dto.ContactMessageResponse;
import org.store.contact.application.dto.ContactReplyRequest;
import org.store.contact.application.service.IContactMessageService;
import org.store.contact.domain.enums.ContactStatut;
import org.store.contact.domain.model.ContactMessage;
import org.store.contact.domain.service.ContactMessageDomainService;
import org.store.notification.domain.enums.CanalNotification;
import org.store.notification.domain.enums.NotificationStatut;
import org.store.notification.domain.model.Notification;
import org.store.notification.domain.service.NotificationDomainService;
import org.store.security.domain.model.Account;
import org.store.security.domain.service.AccountDomainService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles the full contact-message lifecycle: public submission, admin listing,
 * detail view (auto-marks as LU), and admin reply (marks as REPONDU + notifies).
 */
@Service
@Transactional(readOnly = true)
public class ContactMessageServiceImpl implements IContactMessageService {

    private final ContactMessageDomainService contactMessageDomainService;
    private final NotificationDomainService notificationDomainService;
    private final AccountDomainService accountDomainService;
    private final ValidatorService validatorService;

    public ContactMessageServiceImpl(ContactMessageDomainService contactMessageDomainService,
                                     NotificationDomainService notificationDomainService,
                                     AccountDomainService accountDomainService,
                                     ValidatorService validatorService) {
        this.contactMessageDomainService = contactMessageDomainService;
        this.notificationDomainService = notificationDomainService;
        this.accountDomainService = accountDomainService;
        this.validatorService = validatorService;
    }

    @Override
    @Transactional
    public ContactMessageResponse submit(ContactMessageRequest contactMessageRequest) {
        validatorService.validate(contactMessageRequest);

        ContactMessage contactMessage = new ContactMessage();
        contactMessage.setNom(contactMessageRequest.nom());
        contactMessage.setEmail(contactMessageRequest.email());
        contactMessage.setSujet(contactMessageRequest.sujet());
        contactMessage.setMessage(contactMessageRequest.message());

        ContactMessage saved = contactMessageDomainService.save(contactMessage);

        notifyAdmins(saved);

        return new ContactMessageResponse(saved);
    }

    @Override
    public Page<ContactMessageResponse> findAll(Pageable pageable) {
        return contactMessageDomainService.findAll(pageable).map(ContactMessageResponse::new);
    }

    @Override
    @Transactional
    public ContactMessageResponse findById(UUID id) {
        ContactMessage contactMessage = contactMessageDomainService.findById(id);
        if (contactMessage.getStatut() == ContactStatut.NOUVEAU) {
            contactMessage.setStatut(ContactStatut.LU);
            contactMessageDomainService.save(contactMessage);
        }
        return new ContactMessageResponse(contactMessage);
    }

    @Override
    @Transactional
    public ContactMessageResponse reply(UUID id, ContactReplyRequest contactReplyRequest) {
        validatorService.validate(contactReplyRequest);
        ContactMessage contactMessage = contactMessageDomainService.findById(id);
        contactMessage.setReponse(contactReplyRequest.reponse());
        contactMessage.setStatut(ContactStatut.REPONDU);
        return new ContactMessageResponse(contactMessageDomainService.save(contactMessage));
    }

    /** Creates an IN_APP notification for every ADMIN account. */
    private void notifyAdmins(ContactMessage contactMessage) {
        String titre = "Contact : " + contactMessage.getSujet();
        String body = contactMessage.getNom() + " <" + contactMessage.getEmail() + ">\n" + contactMessage.getMessage();

        accountDomainService.findAllByRoleLibelle("ADMIN", org.springframework.data.domain.Pageable.ofSize(100))
                .getContent()
                .forEach(account -> createNotification(account, titre, body));
    }

    private void createNotification(Account destinataire, String titre, String message) {
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
