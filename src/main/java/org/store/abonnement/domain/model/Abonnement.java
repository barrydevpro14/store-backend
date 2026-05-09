package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Entreprise;

import java.time.LocalDate;
@Entity
@Table(name = Abonnement.TABLE_NAME)
public class Abonnement extends AuditableEntity {
    public static final String TABLE_NAME = "abonnement";
    @ManyToOne(fetch = FetchType.LAZY)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    private PlanAbonnement plan;

    @ManyToOne(fetch = FetchType.LAZY)
    private TypeAbonnement typeAbonnement;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private boolean actif = true;

    private boolean renouvellementAuto = false;

    @Enumerated(EnumType.STRING)
    private AbonnementStatut statut;

    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }

    public PlanAbonnement getPlan() {
        return plan;
    }

    public void setPlan(PlanAbonnement plan) {
        this.plan = plan;
    }

    public TypeAbonnement getTypeAbonnement() {
        return typeAbonnement;
    }

    public void setTypeAbonnement(TypeAbonnement typeAbonnement) {
        this.typeAbonnement = typeAbonnement;
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

    public boolean getRenouvellementAuto() {
        return renouvellementAuto;
    }

    public void setRenouvellementAuto(boolean renouvellementAuto) {
        this.renouvellementAuto = renouvellementAuto;
    }

    public AbonnementStatut getStatut() {
        return statut;
    }

    public void setStatut(AbonnementStatut statut) {
        this.statut = statut;
    }
}
