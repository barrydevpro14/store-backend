package org.store.contact.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;
import org.store.contact.application.dto.ContactMessageFilter;
import org.store.contact.application.dto.ContactMessageResponse;
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

    /** Paginated + filtered listing. */
    public Page<ContactMessageResponse> findByFilter(ContactMessageFilter filter) {
        return repository.findResponsesByFilter(
                filter.nom(), LikePatternHelper.toLikePattern(filter.nom()),
                filter.email(), LikePatternHelper.toLikePattern(filter.email()),
                filter.statut(),
                filter.startDate(), filter.endDate(),
                filter.toPageable());
    }
}
