package org.store.contact.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.ValidatorService;
import org.store.contact.application.dto.ContactMessageRequest;
import org.store.contact.application.dto.ContactMessageResponse;
import org.store.contact.application.dto.ContactReplyRequest;
import org.store.contact.application.service.IContactMessageService;
import org.store.contact.domain.enums.ContactStatut;
import org.store.contact.domain.model.ContactMessage;
import org.store.contact.domain.service.ContactMessageDomainService;
import org.store.notification.application.event.ContactMessageReceivedEvent;
import org.store.notification.application.service.INotificationEventPublisher;

import java.util.UUID;

/**
 * Handles the full contact-message lifecycle: public submission, admin listing,
 * detail view (auto-marks as LU), and admin reply (marks as REPONDU).
 * Admin notifications on new messages are fired via ApplicationEvent (NotificationEventListener).
 */
@Service
@Transactional(readOnly = true)
public class ContactMessageServiceImpl implements IContactMessageService {

    private final ContactMessageDomainService contactMessageDomainService;
    private final INotificationEventPublisher notificationEventPublisher;
    private final ValidatorService validatorService;

    public ContactMessageServiceImpl(ContactMessageDomainService contactMessageDomainService,
                                     INotificationEventPublisher notificationEventPublisher,
                                     ValidatorService validatorService) {
        this.contactMessageDomainService = contactMessageDomainService;
        this.notificationEventPublisher = notificationEventPublisher;
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

        notificationEventPublisher.publishContactMessageReceived(new ContactMessageReceivedEvent(saved));

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
}
