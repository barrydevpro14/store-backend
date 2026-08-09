package org.store.stock.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.stock.domain.enums.MouvementStockType;

import java.math.BigDecimal;

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

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal quantite;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal stockAvant;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal stockApres;

    private String referenceDocument;

    private String commentaire;
}
