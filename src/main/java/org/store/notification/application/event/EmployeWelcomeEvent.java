package org.store.notification.application.event;

/** Fired when a new employee account is created with an auto-generated password. */
public record EmployeWelcomeEvent(
        String toEmail,
        String recipientName,
        String username,
        String password) {}
