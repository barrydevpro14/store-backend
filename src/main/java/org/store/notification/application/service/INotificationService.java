package org.store.notification.application.service;

import org.springframework.data.domain.Page;
import org.store.notification.application.dto.NotificationFilter;
import org.store.notification.application.dto.NotificationPayload;
import org.store.notification.application.dto.NotificationResponse;
import org.store.security.domain.model.Account;

import java.util.UUID;

public interface INotificationService {

    Page<NotificationResponse> findAllForCurrentUser(NotificationFilter filter);

    long countUnreadForCurrentUser();

    NotificationResponse markAsRead(UUID id);

    void markAllAsRead();

    /** Creates and persists an IN_APP notification for the given account. */
    void createInApp(Account destinataire, NotificationPayload payload);

    /** Finds the OWNER account for the enterprise and sends an IN_APP notification. */
    void sendInAppToEntreprise(UUID entrepriseId, NotificationPayload payload);
}
