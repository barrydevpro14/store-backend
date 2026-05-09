package org.store.stock.domain.model;

import jakarta.persistence.*;
import org.store.common.base.AuditableEntity;
import org.store.vente.domain.model.LigneCommandeVente;

import java.math.BigDecimal;
@Entity
@Table(name = SortieStock.TABLE_NAME)
public class SortieStock extends AuditableEntity {
    public static final String TABLE_NAME = "sortie_stock";
    @ManyToOne(fetch = FetchType.LAZY)
    private LigneCommandeVente ligneVente;

    @ManyToOne(fetch = FetchType.LAZY)
    private EntreeStock entreeStock;

    private int quantiteSortie;

    @Column(precision = 19, scale = 2)
    private BigDecimal prixAchat;

    @Column(precision = 19, scale = 2)
    private BigDecimal prixVente;

    @Column(precision = 19, scale = 2)
    private BigDecimal marge;

    public LigneCommandeVente getLigneVente() {
        return ligneVente;
    }

    public void setLigneVente(LigneCommandeVente ligneVente) {
        this.ligneVente = ligneVente;
    }

    public EntreeStock getEntreeStock() {
        return entreeStock;
    }

    public void setEntreeStock(EntreeStock entreeStock) {
        this.entreeStock = entreeStock;
    }

    public int getQuantiteSortie() {
        return quantiteSortie;
    }

    public void setQuantiteSortie(int quantiteSortie) {
        this.quantiteSortie = quantiteSortie;
    }

    public BigDecimal getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(BigDecimal prixAchat) {
        this.prixAchat = prixAchat;
    }

    public BigDecimal getPrixVente() {
        return prixVente;
    }

    public void setPrixVente(BigDecimal prixVente) {
        this.prixVente = prixVente;
    }

    public BigDecimal getMarge() {
        return marge;
    }

    public void setMarge(BigDecimal marge) {
        this.marge = marge;
    }
}
