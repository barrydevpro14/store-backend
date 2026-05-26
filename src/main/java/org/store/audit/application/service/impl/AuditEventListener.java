package org.store.audit.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.domain.model.AuditLog;
import org.store.audit.domain.service.AuditLogDomainService;

import java.time.LocalDateTime;

/**
 * Persists AuditLog rows asynchronously from business domain events.
 * Failures are isolated — a logging failure never rolls back the originating transaction.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditLogDomainService auditLogDomainService;

    public AuditEventListener(AuditLogDomainService auditLogDomainService) {
        this.auditLogDomainService = auditLogDomainService;
    }

    @Async
    @EventListener
    public void onAuditEvent(AuditEvent event) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(event.action());
        auditLog.setEntityType(event.entityType());
        auditLog.setEntityId(event.entityId());
        auditLog.setEntityLabel(event.entityLabel());
        auditLog.setPerformedBy(event.performedBy());
        auditLog.setPerformedByLabel(event.performedByLabel());
        auditLog.setEntrepriseId(event.entrepriseId());
        auditLog.setMagasinId(event.magasinId());
        auditLog.setDetails(event.details());
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogDomainService.save(auditLog);
        log.info("Audit: {} {} '{}' by {}", event.action(), event.entityType(), event.entityLabel(), event.performedByLabel());
    }
}
