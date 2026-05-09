package org.store.stock.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.achat.domain.model.CommandeAchat;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = EntreeStock.TABLE_NAME)
public class EntreeStock extends AuditableEntity {
    public static final String TABLE_NAME = "entree_stock";

    @ManyToOne(fetch = FetchType.LAZY)
    private Product produit;

    @ManyToOne(fetch = FetchType.LAZY)
    private ProductFournisseur productFournisseur;

    @ManyToOne(fetch = FetchType.LAZY)
    private Magasin magasin;

    private int quantiteInitiale;

    private int quantiteRestante;

    @Column(precision = 19, scale = 2)
    private BigDecimal prixAchat;

    private String numeroLot;

    private LocalDate dateExpiration;

    @ManyToOne(fetch = FetchType.LAZY)
    private CommandeAchat commandeAchat;
}
