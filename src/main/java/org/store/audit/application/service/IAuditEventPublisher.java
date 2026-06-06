package org.store.audit.application.service;

import org.store.audit.application.event.AuditEvent;

public interface IAuditEventPublisher {
    void publish(AuditEvent event);
}
