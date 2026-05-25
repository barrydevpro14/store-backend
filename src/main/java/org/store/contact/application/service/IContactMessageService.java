package org.store.contact.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.contact.application.dto.ContactMessageRequest;
import org.store.contact.application.dto.ContactMessageResponse;
import org.store.contact.application.dto.ContactReplyRequest;

import java.util.UUID;

public interface IContactMessageService {

    /** Saves a contact message from the public form and notifies all ADMIN accounts. */
    ContactMessageResponse submit(ContactMessageRequest contactMessageRequest);

    /** Paginated listing for ADMIN. */
    Page<ContactMessageResponse> findAll(Pageable pageable);

    /** Single contact message for ADMIN. Marks it as LU on first access. */
    ContactMessageResponse findById(UUID id);

    /** Admin saves a reply and marks the message as REPONDU. */
    ContactMessageResponse reply(UUID id, ContactReplyRequest contactReplyRequest);
}
