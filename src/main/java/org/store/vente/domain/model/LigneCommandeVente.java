package org.store.vente.domain.model;

import jakarta.persistence.*;
import org.store.common.base.BaseEntity;
import org.store.produit.domain.model.Product;

import java.math.BigDecimal;

@Entity
@Table(name = LigneCommandeVente.TABLE_NAME)
public class LigneCommandeVente extends BaseEntity {
    public static final String TABLE_NAME = "ligne_commande_vente";

    @ManyToOne(fetch = FetchType.LAZY)
    private CommandeVente commande;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    private int quantite;

    @Column(precision = 19, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantTotal;

    public CommandeVente getCommande() {
        return commande;
    }

    public void setCommande(CommandeVente commande) {
        this.commande = commande;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(BigDecimal montantTotal) {
        this.montantTotal = montantTotal;
    }
}
