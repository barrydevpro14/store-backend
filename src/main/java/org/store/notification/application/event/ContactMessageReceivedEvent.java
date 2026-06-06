package org.store.notification.application.event;

import org.store.contact.domain.model.ContactMessage;

/** Fired when a visitor submits the public contact form. */
public record ContactMessageReceivedEvent(ContactMessage contactMessage) {}
