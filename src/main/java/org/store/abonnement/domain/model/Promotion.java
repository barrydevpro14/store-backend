package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Table(name = Promotion.TABLE_NAME)
public class Promotion extends AuditableEntity {
    public static final String TABLE_NAME = "promotion";
    private String nom;

    private String description;

    @Enumerated(EnumType.STRING)
    private ReductionType reductionType;

    @Column(precision = 19, scale = 2)
    private BigDecimal valeurReduction;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    private PlanAbonnement plan;

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

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public boolean getActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public PlanAbonnement getPlan() {
        return plan;
    }

    public void setPlan(PlanAbonnement plan) {
        this.plan = plan;
    }
}
