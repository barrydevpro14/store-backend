package org.store.inventaire.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.stock.domain.model.Stock;

@Getter
@Setter
@Entity
@Table(name = LigneInventaire.TABLE_NAME)
public class LigneInventaire extends AuditableEntity {
    public static final String TABLE_NAME = "ligne_inventaire";

    @ManyToOne(fetch = FetchType.LAZY)
    private Inventaire inventaire;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stock stock;

    private int quantiteSysteme;

    private int quantitePhysique;

    private int ecart;
}
