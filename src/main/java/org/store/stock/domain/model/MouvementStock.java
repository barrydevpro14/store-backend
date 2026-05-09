package org.store.stock.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.stock.domain.enums.MouvementStockType;

@Getter
@Setter
@Entity
@Table(name = MouvementStock.TABLE_NAME)
public class MouvementStock extends AuditableEntity {
    public static final String TABLE_NAME = "mouvement_stock";

    @ManyToOne(fetch = FetchType.LAZY)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    private MouvementStockType type;

    private int quantite;

    private int stockAvant;

    private int stockApres;

    private String referenceDocument;

    private String commentaire;
}
