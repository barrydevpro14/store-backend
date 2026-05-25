package org.store.contact.application.dto;

import org.store.common.tools.DateHelper;
import org.store.contact.domain.enums.ContactStatut;
import org.store.contact.domain.model.ContactMessage;

import java.util.UUID;

public record ContactMessageResponse(
        UUID id,
        String nom,
        String email,
        String sujet,
        String message,
        ContactStatut statut,
        String reponse,
        String createdAt
) {
    public ContactMessageResponse(ContactMessage contactMessage) {
        this(
                contactMessage.getId(),
                contactMessage.getNom(),
                contactMessage.getEmail(),
                contactMessage.getSujet(),
                contactMessage.getMessage(),
                contactMessage.getStatut(),
                contactMessage.getReponse(),
                DateHelper.format(contactMessage.getCreatedAt())
        );
    }
}
