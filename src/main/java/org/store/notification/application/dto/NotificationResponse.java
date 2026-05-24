package org.store.notification.application.dto;

import org.store.common.tools.DateHelper;
import org.store.notification.domain.enums.CanalNotification;
import org.store.notification.domain.enums.NotificationStatut;
import org.store.notification.domain.model.Notification;

import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String titre,
        String message,
        CanalNotification canal,
        NotificationStatut statut,
        String dateEnvoi,
        String createdAt
) {
    public NotificationResponse(Notification notification) {
        this(
                notification.getId(),
                notification.getTitre(),
                notification.getMessage(),
                notification.getCanal(),
                notification.getStatut(),
                DateHelper.format(notification.getDateEnvoi()),
                DateHelper.format(notification.getCreatedAt())
        );
    }
}
