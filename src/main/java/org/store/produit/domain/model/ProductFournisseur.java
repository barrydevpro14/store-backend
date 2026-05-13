package org.store.produit.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = ProductFournisseur.TABLE_NAME)
public class ProductFournisseur extends AuditableEntity {
    public static final String TABLE_NAME = "product_fournisseur";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal prixAchat;

    @Column(name = "reference_fournisseur", length = 100)
    private String referenceFournisseur;

    @Column(length = 100)
    private String origine;
}
