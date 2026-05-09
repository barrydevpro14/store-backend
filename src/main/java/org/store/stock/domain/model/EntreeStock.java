package org.store.stock.domain.model;

import jakarta.persistence.*;
import org.store.achat.domain.model.CommandeAchat;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public int getQuantiteInitiale() {
        return quantiteInitiale;
    }

    public void setQuantiteInitiale(int quantiteInitiale) {
        this.quantiteInitiale = quantiteInitiale;
    }

    public int getQuantiteRestante() {
        return quantiteRestante;
    }

    public void setQuantiteRestante(int quantiteRestante) {
        this.quantiteRestante = quantiteRestante;
    }

    public BigDecimal getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(BigDecimal prixAchat) {
        this.prixAchat = prixAchat;
    }

    public String getNumeroLot() {
        return numeroLot;
    }

    public void setNumeroLot(String numeroLot) {
        this.numeroLot = numeroLot;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public CommandeAchat getCommandeAchat() {
        return commandeAchat;
    }

    public void setCommandeAchat(CommandeAchat commandeAchat) {
        this.commandeAchat = commandeAchat;
    }
}