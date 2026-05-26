package org.store.notification.application.event;

/** Fired when an admin saves a reply to a contact message. Carries the data needed to send the reply email. */
public record ContactMessageRepliedEvent(
        String nom,
        String email,
        String sujet,
        String originalMessage,
        String reponse) {}
