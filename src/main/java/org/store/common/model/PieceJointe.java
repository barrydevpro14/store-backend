package org.store.common.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = PieceJointe.TABLE_NAME)
public class PieceJointe extends BaseEntity {
    public static final String TABLE_NAME = "piece_jointe";

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] document;

    private LocalDate date;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;
}
