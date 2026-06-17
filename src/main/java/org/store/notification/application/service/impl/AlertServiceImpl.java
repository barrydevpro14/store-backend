package org.store.notification.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.UserRoleEnum;
import org.store.notification.application.dto.AlerteResponse;
import org.store.notification.application.service.IAlertService;
import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;
import org.store.notification.domain.model.Alerte;
import org.store.notification.domain.service.AlerteDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Service
public class AlertServiceImpl implements IAlertService {

    private final AlerteDomainService alerteDomainService;
    private final ICurrentUserService currentUserService;

    public AlertServiceImpl(AlerteDomainService alerteDomainService, ICurrentUserService currentUserService) {
        this.alerteDomainService = alerteDomainService;
        this.currentUserService = currentUserService;
    }

    @Override
    public Alerte create(AlerteType type, AlerteStatut statut, String titre, String message, UUID entrepriseId, UUID magasinId, UUID entityId, Integer joursInfo) {
        return alerteDomainService.create(type , statut ,titre, message,
                entrepriseId, magasinId, entityId, joursInfo);
    }


    @Override
    public Page<AlerteResponse> findByFilter(UUID entrepriseId, UUID magasinId, AlerteType type, AlerteStatut statut, LocalDateTime from, LocalDateTime to, int page, int size) {
        return alerteDomainService.findByFilter(
                entrepriseId, magasinId, type, statut, from, to, page, size
        );
    }

    /**
     * @param alerte 
     * @return
     */
    @Override
    public Alerte markAsRead(Alerte alerte) {
        return alerteDomainService.markAsRead(alerte);
    }

    /**
     * @param alerte 
     * @return
     */
    @Override
    public Alerte markAsResolved(Alerte alerte) {
        return alerteDomainService.markAsResolved(alerte);
    }

    @Override
    public Alerte findById(UUID id) {
        return alerteDomainService.findById(id);
    }

    @Override
    public Long countNouvelle() {
        UserPrincipal currentUser = currentUserService.getCurrent();
        List<AlerteType> alerteTypes = new ArrayList<>();
        if(currentUser.role().equals(UserRoleEnum.OWNER.name())){
            alerteTypes.add(AlerteType.ABONNEMENT_EXPIRING);
        }
        else if(currentUser.role().equals(UserRoleEnum.MANAGER.name())){
            alerteTypes.addAll(List.of(AlerteType.FACTURE_ACHAT_OVERDUE , AlerteType.FACTURE_VENTE_OVERDUE));
        }else {
            return 0L;
        }
        return alerteDomainService.countNouvelles(currentUser.entrepriseId() , currentUser.magasinId(), alerteTypes);
    }
}
