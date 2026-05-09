package org.store.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.achat.domain.model.FactureAchat;
import org.store.common.base.AuditableEntity;
import org.store.notification.domain.enums.CanalNotification;
import org.store.notification.domain.enums.NotificationStatut;
import org.store.security.domain.model.Account;
import org.store.vente.domain.model.FactureClient;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = Notification.TABLE_NAME)
public class Notification extends AuditableEntity {
    public static final String TABLE_NAME = "notification";

    private String titre;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private CanalNotification canal;

    @Enumerated(EnumType.STRING)
    private NotificationStatut statut;

    private LocalDateTime dateEnvoi;

    private LocalDate prochaineTentative;

    private int nombreTentatives = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    private Account destinataire;

    @ManyToOne(fetch = FetchType.LAZY)
    private FactureClient factureClient;

    @ManyToOne(fetch = FetchType.LAZY)
    private FactureAchat factureAchat;

    @ManyToOne(fetch = FetchType.LAZY)
    private Echeance echeance;
}
