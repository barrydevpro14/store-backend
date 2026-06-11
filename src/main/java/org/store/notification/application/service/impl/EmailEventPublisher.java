package org.store.notification.application.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.store.notification.application.event.EmployeWelcomeEvent;
import org.store.notification.application.event.OwnerWelcomeEvent;
import org.store.notification.application.event.PasswordResetRequestedEvent;
import org.store.notification.application.service.IEmailEventPublisher;

/**
 * Delegates email business events to the Spring ApplicationEventPublisher.
 * Each method fires a typed event record consumed asynchronously by EmailEventListener.
 */
@Service
public class EmailEventPublisher implements IEmailEventPublisher {

    private final ApplicationEventPublisher publisher;

    public EmailEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishPasswordResetRequested(PasswordResetRequestedEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishEmployeWelcome(EmployeWelcomeEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishOwnerWelcome(OwnerWelcomeEvent event) {
        publisher.publishEvent(event);
    }
}
