package org.store.contact.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.contact.application.dto.ContactMessageResponse;
import org.store.contact.domain.enums.ContactStatut;
import org.store.contact.domain.model.ContactMessage;

public interface ContactMessageRepository extends BaseRepository<ContactMessage> {

    @Query("SELECT COUNT(c) FROM ContactMessage c WHERE c.statut = :statut")
    long countByStatut(@Param("statut") ContactStatut statut);

    @Query(value = """
            SELECT new org.store.contact.application.dto.ContactMessageResponse(c)
            FROM ContactMessage c
            WHERE (:nom IS NULL OR :nom = '' OR LOWER(c.nom) LIKE :nomPattern)
              AND (:email IS NULL OR :email = '' OR LOWER(c.email) LIKE :emailPattern)
              AND (:statut IS NULL OR c.statut = :statut)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', c.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', c.createdAt) <= CAST(:endDate AS date))
            ORDER BY c.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(c)
            FROM ContactMessage c
            WHERE (:nom IS NULL OR :nom = '' OR LOWER(c.nom) LIKE :nomPattern)
              AND (:email IS NULL OR :email = '' OR LOWER(c.email) LIKE :emailPattern)
              AND (:statut IS NULL OR c.statut = :statut)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', c.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', c.createdAt) <= CAST(:endDate AS date))
            """)
    Page<ContactMessageResponse> findResponsesByFilter(
            @Param("nom") String nom,
            @Param("nomPattern") String nomPattern,
            @Param("email") String email,
            @Param("emailPattern") String emailPattern,
            @Param("statut") ContactStatut statut,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);
}
