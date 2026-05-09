package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
@Entity
@Table(name = TypeAbonnement.TABLE_NAME)
public class TypeAbonnement extends AuditableEntity {
    public static final String TABLE_NAME = "type_abonnement";
    @Column(nullable = false, unique = true)
    private String nom;

    private int dureeMois;

    @Enumerated(EnumType.STRING)
    private ReductionType reductionType;

    @Column(precision = 19, scale = 2)
    private BigDecimal valeurReduction;

    private boolean recommande = false;

    private boolean actif = true;

    private int ordre;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public int getDureeMois() {
        return dureeMois;
    }

    public void setDureeMois(int dureeMois) {
        this.dureeMois = dureeMois;
    }

    public ReductionType getReductionType() {
        return reductionType;
    }

    public void setReductionType(ReductionType reductionType) {
        this.reductionType = reductionType;
    }

    public BigDecimal getValeurReduction() {
        return valeurReduction;
    }

    public void setValeurReduction(BigDecimal valeurReduction) {
        this.valeurReduction = valeurReduction;
    }

    public boolean getRecommande() {
        return recommande;
    }

    public void setRecommande(boolean recommande) {
        this.recommande = recommande;
    }

    public boolean getActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }
}