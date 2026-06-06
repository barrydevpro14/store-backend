package org.store.notification.application.service;

import org.springframework.data.domain.Page;
import org.store.notification.application.dto.NotificationFilter;
import org.store.notification.application.dto.NotificationResponse;

import java.util.UUID;

public interface INotificationService {

    Page<NotificationResponse> findAllForCurrentUser(NotificationFilter filter);

    long countUnreadForCurrentUser();

    NotificationResponse markAsRead(UUID id);

    void markAllAsRead();
}
