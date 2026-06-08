package org.store.notification.application.dto;

import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;
import org.store.notification.domain.model.Alerte;
import org.store.common.tools.DateHelper;

import java.util.UUID;

public record AlerteResponse(
        UUID id,
        AlerteType type,
        AlerteStatut statut,
        String titre,
        String message,
        UUID entrepriseId,
        UUID magasinId,
        UUID entityId,
        Integer joursInfo,
        String createdAt
) {
    public AlerteResponse(Alerte a) {
        this(a.getId(), a.getType(), a.getStatut(), a.getTitre(), a.getMessage(),
             a.getEntrepriseId(), a.getMagasinId(), a.getEntityId(),
             a.getJoursInfo(), DateHelper.format(a.getCreatedAt()));
    }
}
