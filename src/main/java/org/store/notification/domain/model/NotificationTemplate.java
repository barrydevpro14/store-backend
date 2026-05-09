package org.store.notification.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.notification.domain.enums.CanalNotification;

@Getter
@Setter
public class NotificationTemplate extends AuditableEntity {

    @Column(unique = true)
    private String code;

    private String sujet;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    @Enumerated(EnumType.STRING)
    private CanalNotification canal;

    private boolean actif = true;
}
