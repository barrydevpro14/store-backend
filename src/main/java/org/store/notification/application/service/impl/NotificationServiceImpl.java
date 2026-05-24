package org.store.notification.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.notification.application.dto.NotificationResponse;
import org.store.notification.application.service.INotificationService;
import org.store.notification.domain.model.Notification;
import org.store.notification.domain.service.NotificationDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

/**
 * Gère les notifications de l'utilisateur courant : lecture paginée,
 * marquage lu / tout lu, compteur de non-lues.
 */
@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements INotificationService {

    private final NotificationDomainService notificationDomainService;
    private final ICurrentUserService currentUserService;

    public NotificationServiceImpl(NotificationDomainService notificationDomainService,
                                   ICurrentUserService currentUserService) {
        this.notificationDomainService = notificationDomainService;
        this.currentUserService = currentUserService;
    }

    @Override
    public Page<NotificationResponse> findAllForCurrentUser(Pageable pageable) {
        UUID accountId = currentUserService.getCurrent().accountId();
        return notificationDomainService.findByDestinataire(accountId, pageable)
                .map(NotificationResponse::new);
    }

    @Override
    public long countUnreadForCurrentUser() {
        UUID accountId = currentUserService.getCurrent().accountId();
        return notificationDomainService.countUnread(accountId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID id) {
        UserPrincipal current = currentUserService.getCurrent();
        Notification notification = notificationDomainService.findById(id);

        if (!notification.getDestinataire().getId().equals(current.accountId())) {
            throw new ForbiddenException("notification.notOwned");
        }

        return new NotificationResponse(notificationDomainService.markAsRead(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        UUID accountId = currentUserService.getCurrent().accountId();
        notificationDomainService.markAllAsRead(accountId);
    }
}
