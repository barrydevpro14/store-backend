package org.store.notification.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.notification.application.dto.NotificationResponse;

import java.util.UUID;

public interface INotificationService {

    Page<NotificationResponse> findAllForCurrentUser(Pageable pageable);

    long countUnreadForCurrentUser();

    NotificationResponse markAsRead(UUID id);

    void markAllAsRead();
}
