package org.store.common.model;

import jakarta.persistence.*;
import org.store.common.base.BaseEntity;

import java.time.LocalDate;

@Entity
@Table(name = PieceJointe.TABLE_NAME)
public class PieceJointe extends BaseEntity {
    public static final String TABLE_NAME = "piece_jointe";
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] document;
    private LocalDate date;

    public byte[] getDocument() {
        return document;
    }

    public void setDocument(byte[] document) {
        this.document = document;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
