package org.store.inventaire.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.store.common.base.AuditableEntity;
import org.store.stock.domain.model.Stock;
@Entity
@Table(name = LigneInventaire.TABLE_NAME)
public class LigneInventaire extends AuditableEntity {
    public static final String TABLE_NAME = "ligne_inventaire";
    @ManyToOne(fetch = FetchType.LAZY)
    private Inventaire inventaire;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stock stock;

    private int quantiteSysteme;

    private int quantitePhysique;

    private int ecart;

    public Inventaire getInventaire() {
        return inventaire;
    }

    public void setInventaire(Inventaire inventaire) {
        this.inventaire = inventaire;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public int getQuantiteSysteme() {
        return quantiteSysteme;
    }

    public void setQuantiteSysteme(int quantiteSysteme) {
        this.quantiteSysteme = quantiteSysteme;
    }

    public int getQuantitePhysique() {
        return quantitePhysique;
    }

    public void setQuantitePhysique(int quantitePhysique) {
        this.quantitePhysique = quantitePhysique;
    }

    public int getEcart() {
        return ecart;
    }

    public void setEcart(int ecart) {
        this.ecart = ecart;
    }
}
