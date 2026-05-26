package org.store.notification.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.notification.application.dto.NotificationFilter;
import org.store.notification.domain.enums.NotificationStatut;
import org.store.notification.domain.model.Notification;
import org.store.notification.domain.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationDomainService extends GlobalService<Notification, NotificationRepository> {

    private static final List<NotificationStatut> UNREAD_STATUTS =
            List.of(NotificationStatut.EN_ATTENTE, NotificationStatut.ENVOYEE);

    public NotificationDomainService(NotificationRepository repository) {
        super(repository);
    }

    public Page<Notification> findByDestinataire(UUID accountId, Pageable pageable) {
        return repository.findByDestinataire(accountId, pageable);
    }

    public Page<Notification> findByFilter(UUID accountId, NotificationFilter filter) {
        return repository.findByFilter(accountId, filter, filter.toPageable());
    }

    public long countUnread(UUID accountId) {
        return repository.countUnread(accountId, UNREAD_STATUTS);
    }

    public Notification markAsRead(Notification notification) {
        notification.setStatut(NotificationStatut.LUE);
        notification.setDateEnvoi(LocalDateTime.now());
        return save(notification);
    }

    public void markAllAsRead(UUID accountId) {
        repository.markAllAsRead(accountId, NotificationStatut.LUE, LocalDateTime.now(), UNREAD_STATUTS);
    }
}
