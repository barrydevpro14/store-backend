package org.store.contact.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.contact.domain.model.ContactMessage;
import org.store.contact.domain.repository.ContactMessageRepository;

import java.util.UUID;

public interface ContactMessageJpaRepository
        extends JpaRepository<ContactMessage, UUID>, ContactMessageRepository {
}
