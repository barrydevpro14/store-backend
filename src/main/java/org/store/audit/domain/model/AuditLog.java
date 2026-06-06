package org.store.audit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.common.base.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = AuditLog.TABLE_NAME)
public class AuditLog extends BaseEntity {

    public static final String TABLE_NAME = "audit_log";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditEntityType entityType;

    private UUID entityId;

    private String entityLabel;

    @Column(nullable = false)
    private String performedBy;

    private String performedByLabel;

    private UUID entrepriseId;

    private String entrepriseLabel;

    private UUID magasinId;

    private String magasinLabel;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
