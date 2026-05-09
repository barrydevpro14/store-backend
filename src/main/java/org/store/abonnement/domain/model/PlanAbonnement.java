package org.store.abonnement.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
@Entity
@Table(name = PlanAbonnement.TABLE_NAME)
public class PlanAbonnement extends AuditableEntity {
    public static final String TABLE_NAME = "plan_abonnement";
    @Column(nullable = false, unique = true)
    private String nom;

    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal prix;

    private int nombreMagasinsMax;

    private int nombreEmployesMax;

    private boolean gestionStock = true;

    private boolean gestionVente = true;

    private boolean gestionAchat = true;

    private boolean gestionComptabilite = false;

    private boolean actif = true;

    private boolean visible = true;

    private boolean trial = false;

    private int ordre;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public int getNombreMagasinsMax() {
        return nombreMagasinsMax;
    }

    public void setNombreMagasinsMax(int nombreMagasinsMax) {
        this.nombreMagasinsMax = nombreMagasinsMax;
    }

    public int getNombreEmployesMax() {
        return nombreEmployesMax;
    }

    public void setNombreEmployesMax(int nombreEmployesMax) {
        this.nombreEmployesMax = nombreEmployesMax;
    }

    public boolean getGestionStock() {
        return gestionStock;
    }

    public void setGestionStock(boolean gestionStock) {
        this.gestionStock = gestionStock;
    }

    public boolean getGestionVente() {
        return gestionVente;
    }

    public void setGestionVente(boolean gestionVente) {
        this.gestionVente = gestionVente;
    }

    public boolean getGestionAchat() {
        return gestionAchat;
    }

    public void setGestionAchat(boolean gestionAchat) {
        this.gestionAchat = gestionAchat;
    }

    public boolean getGestionComptabilite() {
        return gestionComptabilite;
    }

    public void setGestionComptabilite(boolean gestionComptabilite) {
        this.gestionComptabilite = gestionComptabilite;
    }

    public boolean getActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean getTrial() {
        return trial;
    }

    public void setTrial(boolean trial) {
        this.trial = trial;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }
}
