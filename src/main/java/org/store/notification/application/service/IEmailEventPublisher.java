package org.store.notification.application.service;

import org.store.notification.application.event.EmployeWelcomeEvent;
import org.store.notification.application.event.OwnerWelcomeEvent;
import org.store.notification.application.event.PasswordResetRequestedEvent;

public interface IEmailEventPublisher {
    void publishPasswordResetRequested(PasswordResetRequestedEvent event);
    void publishEmployeWelcome(EmployeWelcomeEvent event);
    void publishOwnerWelcome(OwnerWelcomeEvent event);
}
