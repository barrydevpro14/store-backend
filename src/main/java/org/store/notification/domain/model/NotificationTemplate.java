package org.store.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.notification.domain.enums.CanalNotification;

@Getter
@Setter
@Entity
@Table(name = NotificationTemplate.TABLE_NAME)
public class NotificationTemplate extends AuditableEntity {
    public static final String TABLE_NAME = "notification_template";
    @Column(unique = true)
    private String code;

    private String sujet;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    @Enumerated(EnumType.STRING)
    private CanalNotification canal;

    private boolean actif = true;
}
