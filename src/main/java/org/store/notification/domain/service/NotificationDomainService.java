package org.store.notification.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.notification.domain.model.Notification;
import org.store.notification.domain.repository.NotificationJpaRepository;

@Service
public class NotificationDomainService extends GlobalService<Notification, NotificationJpaRepository> {
    public NotificationDomainService(NotificationJpaRepository repository) {
        super(repository);
    }
}
