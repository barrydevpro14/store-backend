package org.store.stock.domain.model;

import jakarta.persistence.*;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;
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

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public Product getProduit() {
        return produit;
    }

    public void setProduit(Product produit) {
        this.produit = produit;
    }

    public ProductFournisseur getProductFournisseur() {
        return productFournisseur;
    }

    public void setProductFournisseur(ProductFournisseur productFournisseur) {
        this.productFournisseur = productFournisseur;
    }

    public int getQuantiteDisponible() {
        return quantiteDisponible;
    }

    public void setQuantiteDisponible(int quantiteDisponible) {
        this.quantiteDisponible = quantiteDisponible;
    }

    public int getSeuilApprovisionnement() {
        return seuilApprovisionnement;
    }

    public void setSeuilApprovisionnement(int seuilApprovisionnement) {
        this.seuilApprovisionnement = seuilApprovisionnement;
    }

    public BigDecimal getPrixAchatMoyen() {
        return prixAchatMoyen;
    }

    public void setPrixAchatMoyen(BigDecimal prixAchatMoyen) {
        this.prixAchatMoyen = prixAchatMoyen;
    }
}
