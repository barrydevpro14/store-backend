package org.store.stock.domain.model;

import jakarta.persistence.*;
import org.store.common.base.AuditableEntity;
import org.store.stock.domain.enums.MouvementStockType;

@Entity
@Table(name = MouvementStock.TABLE_NAME)
public class MouvementStock extends AuditableEntity {
    public static final String TABLE_NAME = "mouvement_stock";
    @ManyToOne(fetch = FetchType.LAZY)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    private MouvementStockType type;

    private int quantite;

    private int stockAvant;

    private int stockApres;

    private String referenceDocument;

    private String commentaire;

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public MouvementStockType getType() {
        return type;
    }

    public void setType(MouvementStockType type) {
        this.type = type;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public int getStockAvant() {
        return stockAvant;
    }

    public void setStockAvant(int stockAvant) {
        this.stockAvant = stockAvant;
    }

    public int getStockApres() {
        return stockApres;
    }

    public void setStockApres(int stockApres) {
        this.stockApres = stockApres;
    }

    public String getReferenceDocument() {
        return referenceDocument;
    }

    public void setReferenceDocument(String referenceDocument) {
        this.referenceDocument = referenceDocument;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}
