package org.store.contact.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.contact.domain.enums.ContactStatut;

@Getter
@Setter
@Entity
@Table(name = ContactMessage.TABLE_NAME)
public class ContactMessage extends AuditableEntity {
    public static final String TABLE_NAME = "contact_message";

    private String nom;
    private String email;
    private String sujet;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private ContactStatut statut = ContactStatut.NOUVEAU;

    @Column(columnDefinition = "TEXT")
    private String reponse;
}
