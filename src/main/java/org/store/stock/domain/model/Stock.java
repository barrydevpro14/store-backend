package org.store.stock.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = Stock.TABLE_NAME, uniqueConstraints = @UniqueConstraint(name = "uk_stock_magasin_produit", columnNames = {"magasin_id", "produit_id"}))
public class Stock extends AuditableEntity {
    public static final String TABLE_NAME = "stock";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Magasin magasin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Product produit;

    private int quantiteDisponible = 0;

    private int seuilApprovisionnement;

    @Column(precision = 19, scale = 2)
    private BigDecimal prixAchatMoyen;
}
