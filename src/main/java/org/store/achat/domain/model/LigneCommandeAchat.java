package org.store.achat.domain.model;

import jakarta.persistence.*;
import org.store.common.base.AuditableEntity;
import org.store.common.base.BaseEntity;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;

@Entity
@Table(name = LigneCommandeAchat.TABLE_NAME)
public class LigneCommandeAchat extends BaseEntity {
    public static final String TABLE_NAME = "ligne_commande_achat";

    @ManyToOne(fetch = FetchType.LAZY)
    private CommandeAchat commande;

    @ManyToOne(fetch = FetchType.LAZY)
    private ProductFournisseur productFournisseur;

    private int quantite;

    @Column(precision = 19, scale = 2)
    private BigDecimal prixAchat;

    public CommandeAchat getCommande() {
        return commande;
    }

    public void setCommande(CommandeAchat commande) {
        this.commande = commande;
    }

    public ProductFournisseur getProductFournisseur() {
        return productFournisseur;
    }

    public void setProductFournisseur(ProductFournisseur productFournisseur) {
        this.productFournisseur = productFournisseur;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(BigDecimal prixAchat) {
        this.prixAchat = prixAchat;
    }
}
