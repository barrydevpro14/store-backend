package org.store.notification.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.common.tools.OwnershipHelper;
import org.store.notification.application.dto.NotificationFilter;
import org.store.notification.application.dto.NotificationPayload;
import org.store.notification.application.dto.NotificationResponse;
import org.store.notification.application.service.INotificationService;
import org.store.notification.domain.enums.CanalNotification;
import org.store.notification.domain.enums.NotificationStatut;
import org.store.notification.domain.model.Notification;
import org.store.notification.domain.service.NotificationDomainService;
import org.store.security.application.service.ICurrentUserService;
import org.store.security.domain.model.Account;
import org.store.users.application.service.IProprietaireService;

import java.time.LocalDateTime;
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
    private final IProprietaireService proprietaireService;

    public NotificationServiceImpl(NotificationDomainService notificationDomainService,
                                   ICurrentUserService currentUserService,
                                   ValidatorService validatorService,
                                   IProprietaireService proprietaireService) {
        this.notificationDomainService = notificationDomainService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
        this.proprietaireService = proprietaireService;
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

    @Override
    @Transactional
    public void createInApp(Account destinataire, NotificationPayload payload) {
        Notification notification = new Notification();
        notification.setDestinataire(destinataire);
        notification.setTitre(payload.titre());
        notification.setMessage(payload.message());
        notification.setContact(payload.contact());
        notification.setCanal(CanalNotification.IN_APP);
        notification.setStatut(NotificationStatut.ENVOYEE);
        notification.setDateEnvoi(LocalDateTime.now());

        notificationDomainService.save(notification);
    }

    @Override
    @Transactional
    public void sendInAppToEntreprise(UUID entrepriseId, NotificationPayload payload) {
        proprietaireService.findAccountByEntrepriseId(entrepriseId)
                .ifPresent(account -> createInApp(account, payload));
    }
}
