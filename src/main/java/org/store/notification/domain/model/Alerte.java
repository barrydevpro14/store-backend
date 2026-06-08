package org.store.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = Alerte.TABLE_NAME)
public class Alerte extends AuditableEntity {
    public static final String TABLE_NAME = "alerte";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private AlerteType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlerteStatut statut = AlerteStatut.NOUVELLE;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String message;

    /** Company the alert belongs to (for scoping). */
    @Column(name = "entreprise_id")
    private UUID entrepriseId;

    /** Store the alert relates to (nullable). */
    @Column(name = "magasin_id")
    private UUID magasinId;

    /** ID of the entity that triggered the alert (abonnement, facture, stock…). */
    @Column(name = "entity_id")
    private UUID entityId;

    /** Number of days remaining (expiry) or overdue (invoice), for display and sorting. */
    @Column(name = "jours_info")
    private Integer joursInfo;
}
