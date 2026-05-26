package org.store.audit.application.event;

import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;

import java.util.UUID;

/**
 * Carries all context needed to persist an AuditLog row.
 * User identity is captured at publish time (main thread) so the async
 * listener does not need access to the SecurityContext.
 */
public record AuditEvent(
        AuditAction action,
        AuditEntityType entityType,
        UUID entityId,
        String entityLabel,
        String performedBy,
        String performedByLabel,
        UUID entrepriseId,
        String details) {}
