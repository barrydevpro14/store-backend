package org.store.stock.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = Stock.TABLE_NAME)
public class Stock extends AuditableEntity {
    public static final String TABLE_NAME = "stock";

    @ManyToOne(fetch = FetchType.LAZY)
    private Magasin magasin;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product produit;

    @ManyToOne(fetch = FetchType.LAZY)
    private ProductFournisseur productFournisseur;

    private int quantiteDisponible = 0;

    private int seuilApprovisionnement;

    @Column(precision = 19, scale = 2)
    private BigDecimal prixAchatMoyen;
}
