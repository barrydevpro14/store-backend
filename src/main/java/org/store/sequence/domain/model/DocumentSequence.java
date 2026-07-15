package org.store.sequence.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.sequence.domain.enums.TypeDocument;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = DocumentSequence.TABLE_NAME)
public class DocumentSequence extends AuditableEntity {

    public static final String TABLE_NAME = "document_sequence";

    @Column(nullable = false)
    private UUID magasinId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDocument typeDocument;

    @Column(nullable = false)
    private String prefixe;

    @Column(nullable = false)
    private long prochaineSequence;

    @Column(nullable = false)
    private int longueurSequence;
}
