package org.store.notification.application.event;

/** Fired when an owner requests a password reset link. */
public record PasswordResetRequestedEvent(
        String toEmail,
        String recipientName,
        String resetLink) {}
