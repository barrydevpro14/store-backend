package org.store.notification.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.notification.domain.model.Notification;
import org.store.notification.domain.repository.NotificationRepository;

@Service
public class NotificationDomainService extends GlobalService<Notification, NotificationRepository> {
    public NotificationDomainService(NotificationRepository repository) {
        super(repository);
    }
}
