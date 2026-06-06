package org.store.notification.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.common.tools.OwnershipHelper;
import org.store.notification.application.dto.NotificationFilter;
import org.store.notification.application.dto.NotificationResponse;
import org.store.notification.application.service.INotificationService;
import org.store.notification.domain.model.Notification;
import org.store.notification.domain.service.NotificationDomainService;
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
    private final ValidatorService validatorService;

    public NotificationServiceImpl(NotificationDomainService notificationDomainService,
                                   ICurrentUserService currentUserService,
                                   ValidatorService validatorService) {
        this.notificationDomainService = notificationDomainService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    @Override
    public Page<NotificationResponse> findAllForCurrentUser(NotificationFilter filter) {
        validatorService.validate(filter);
        UUID accountId = currentUserService.getCurrent().accountId();
        return notificationDomainService.findByFilter(accountId, filter)
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
        Notification notification = notificationDomainService.findById(id);

        OwnershipHelper.ensureOwnership(
                notification,
                notification.getDestinataire().getId(),
                currentUserService.getCurrent().accountId(),
                "notification.notOwned"
        );

        return new NotificationResponse(notificationDomainService.markAsRead(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        UUID accountId = currentUserService.getCurrent().accountId();
        notificationDomainService.markAllAsRead(accountId);
    }
}
