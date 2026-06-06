package org.store.notification.application.event;

/** Fired when a new owner account is registered. */
public record OwnerWelcomeEvent(
        String toEmail,
        String recipientName,
        String username,
        String entrepriseName,
        String loginUrl) {}
