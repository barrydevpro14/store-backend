package org.store.contact.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.contact.application.dto.ContactMessageFilter;
import org.store.contact.application.dto.ContactMessageResponse;
import org.store.contact.domain.enums.ContactStatut;
import org.store.contact.domain.model.ContactMessage;

public interface ContactMessageRepository extends BaseRepository<ContactMessage> {

    @Query("SELECT COUNT(c) FROM ContactMessage c WHERE c.statut = :statut")
    long countByStatut(@Param("statut") ContactStatut statut);

    @Query(value = """
            SELECT new org.store.contact.application.dto.ContactMessageResponse(c)
            FROM ContactMessage c
            WHERE (:#{#filter.nom}    IS NULL OR LOWER(c.nom)   LIKE LOWER(CONCAT('%', :#{#filter.nom},   '%')))
              AND (:#{#filter.email}  IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :#{#filter.email}, '%')))
              AND (:#{#filter.statut} IS NULL OR c.statut = :#{#filter.statut})
              AND c.createdAt >= :#{#filter.createdStartDateTime()}
              AND c.createdAt <  :#{#filter.createdEndDateTime()}
            ORDER BY c.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(c)
            FROM ContactMessage c
            WHERE (:#{#filter.nom}    IS NULL OR LOWER(c.nom)   LIKE LOWER(CONCAT('%', :#{#filter.nom},   '%')))
              AND (:#{#filter.email}  IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :#{#filter.email}, '%')))
              AND (:#{#filter.statut} IS NULL OR c.statut = :#{#filter.statut})
              AND c.createdAt >= :#{#filter.createdStartDateTime()}
              AND c.createdAt <  :#{#filter.createdEndDateTime()}
            """)
    Page<ContactMessageResponse> findResponsesByFilter(@Param("filter") ContactMessageFilter filter,
                                                       Pageable pageable);
}
