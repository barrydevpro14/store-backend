package org.store.contact.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.contact.domain.enums.ContactStatut;
import org.store.contact.domain.model.ContactMessage;

public interface ContactMessageRepository extends BaseRepository<ContactMessage> {

    @Query("SELECT COUNT(c) FROM ContactMessage c WHERE c.statut = :statut")
    long countByStatut(@Param("statut") ContactStatut statut);
}
