package org.store.notification.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.notification.domain.enums.NotificationStatut;
import org.store.notification.domain.model.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends BaseRepository<Notification> {

    @Query("""
            SELECT n FROM Notification n
            WHERE n.destinataire.id = :accountId
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> findByDestinataire(@Param("accountId") UUID accountId, Pageable pageable);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.destinataire.id = :accountId
              AND n.statut IN :statuts
            """)
    long countUnread(@Param("accountId") UUID accountId, @Param("statuts") List<NotificationStatut> statuts);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.statut = :statut, n.dateEnvoi = :now
            WHERE n.destinataire.id = :accountId
              AND n.statut IN :unreadStatuts
            """)
    void markAllAsRead(@Param("accountId") UUID accountId,
                       @Param("statut") NotificationStatut statut,
                       @Param("now") LocalDateTime now,
                       @Param("unreadStatuts") List<NotificationStatut> unreadStatuts);
}
