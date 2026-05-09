package org.store.produit.domain.model;

import jakarta.persistence.*;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
@Entity
@Table(name = ProductFournisseur.TABLE_NAME)
public class ProductFournisseur extends AuditableEntity {
    public static final String TABLE_NAME = "product_fournisseur";
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    private Fournisseur fournisseur;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal prixAchat;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Fournisseur getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
    }

    public BigDecimal getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(BigDecimal prixAchat) {
        this.prixAchat = prixAchat;
    }
}
