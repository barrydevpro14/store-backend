package org.store.contact.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.contact.domain.enums.ContactStatut;
import org.store.contact.domain.model.ContactMessage;
import org.store.contact.domain.repository.ContactMessageRepository;

@Service
public class ContactMessageDomainService extends GlobalService<ContactMessage, ContactMessageRepository> {
    public ContactMessageDomainService(ContactMessageRepository repository) {
        super(repository);
    }

    /** Compte les messages de contact dans un statut donné. */
    public long countByStatut(ContactStatut statut) {
        return repository.countByStatut(statut);
    }
}
