package org.store.audit.application.dto;

import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.audit.domain.model.AuditLog;
import org.store.common.tools.DateHelper;

import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        AuditAction action,
        AuditEntityType entityType,
        UUID entityId,
        String entityLabel,
        String performedBy,
        String performedByLabel,
        UUID entrepriseId,
        UUID magasinId,
        String details,
        String createdAt
) {
    public AuditLogResponse(AuditLog log) {
        this(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getEntityLabel(),
                log.getPerformedBy(),
                log.getPerformedByLabel(),
                log.getEntrepriseId(),
                log.getMagasinId(),
                log.getDetails(),
                DateHelper.format(log.getCreatedAt())
        );
    }
}
