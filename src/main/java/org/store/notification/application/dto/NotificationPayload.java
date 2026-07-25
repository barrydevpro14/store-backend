package org.store.notification.application.dto;

import org.store.contact.domain.model.ContactMessage;

public record NotificationPayload(String titre, String message, ContactMessage contact) {}
