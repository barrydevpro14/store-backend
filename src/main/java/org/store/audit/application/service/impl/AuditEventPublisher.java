package org.store.audit.application.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;

/** Delegates audit events to the Spring ApplicationEventPublisher for async persistence. */
@Service
public class AuditEventPublisher implements IAuditEventPublisher {

    private final ApplicationEventPublisher publisher;

    public AuditEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(AuditEvent event) {
        publisher.publishEvent(event);
    }
}
