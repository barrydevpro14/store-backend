package org.store.notification.application.service;

import org.springframework.data.domain.Page;
import org.store.notification.application.dto.AlerteResponse;
import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;
import org.store.notification.domain.model.Alerte;

import java.time.LocalDateTime;
import java.util.UUID;

public interface IAlertService {
     Alerte create(AlerteType type, AlerteStatut statut, String titre, String message,
                         UUID entrepriseId, UUID magasinId, UUID entityId, Integer joursInfo);

     Page<AlerteResponse> findByFilter(UUID entrepriseId, UUID magasinId,
                                             AlerteType type, AlerteStatut statut,
                                             LocalDateTime from, LocalDateTime to,
                                             int page, int size);

    Alerte markAsRead(Alerte alerte);

    Alerte markAsResolved(Alerte alerte);
    Alerte findById(UUID id);

    Long countNouvelle();
}
