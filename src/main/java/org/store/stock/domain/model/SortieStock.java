package org.store.stock.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.vente.domain.model.LigneCommandeVente;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = SortieStock.TABLE_NAME)
public class SortieStock extends AuditableEntity {
    public static final String TABLE_NAME = "sortie_stock";

    @ManyToOne(fetch = FetchType.LAZY)
    private LigneCommandeVente ligneVente;

    @ManyToOne(fetch = FetchType.LAZY)
    private EntreeStock entreeStock;

    private int quantiteSortie;

    @Column(precision = 19, scale = 2)
    private BigDecimal prixAchat;

    @Column(precision = 19, scale = 2)
    private BigDecimal prixVente;

    @Column(precision = 19, scale = 2)
    private BigDecimal marge;

    @Column(nullable = false)
    private boolean annulee;
}
