package org.store.audit.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.domain.model.AuditLog;
import org.store.audit.domain.service.AuditLogDomainService;
import org.store.entreprise.domain.service.EntrepriseDomainService;
import org.store.magasin.domain.service.MagasinDomainService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persists AuditLog rows asynchronously from business domain events.
 * Resolves entreprise.sigle and magasin.nom at write time so the log is
 * self-contained and human-readable without further lookups.
 * Failures are isolated — a logging failure never rolls back the originating transaction.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditLogDomainService auditLogDomainService;
    private final EntrepriseDomainService entrepriseDomainService;
    private final MagasinDomainService magasinDomainService;

    public AuditEventListener(AuditLogDomainService auditLogDomainService,
                               EntrepriseDomainService entrepriseDomainService,
                               MagasinDomainService magasinDomainService) {
        this.auditLogDomainService = auditLogDomainService;
        this.entrepriseDomainService = entrepriseDomainService;
        this.magasinDomainService = magasinDomainService;
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
        auditLog.setEntrepriseLabel(resolveEntrepriseLabel(event.entrepriseId()));
        auditLog.setMagasinId(event.magasinId());
        auditLog.setMagasinLabel(resolveMagasinLabel(event.magasinId()));
        auditLog.setDetails(event.details());
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogDomainService.save(auditLog);
        log.info("Audit: {} {} '{}' by {}", event.action(), event.entityType(), event.entityLabel(), event.performedByLabel());
    }

    private String resolveEntrepriseLabel(UUID entrepriseId) {
        if (entrepriseId == null) return null;
        try {
            return entrepriseDomainService.findById(entrepriseId).getSigle();
        } catch (Exception e) {
            return entrepriseId.toString();
        }
    }

    private String resolveMagasinLabel(UUID magasinId) {
        if (magasinId == null) return null;
        try {
            return magasinDomainService.findById(magasinId).getNom();
        } catch (Exception e) {
            return magasinId.toString();
        }
    }
}
