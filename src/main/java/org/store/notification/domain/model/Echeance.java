package org.store.notification.domain.model;

import jakarta.persistence.*;
import org.store.abonnement.domain.model.Abonnement;
import org.store.common.base.AuditableEntity;
import org.store.notification.domain.enums.EcheanceStatut;

import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Table(name = Echeance.TABLE_NAME)
public class Echeance extends AuditableEntity {
    public static final String TABLE_NAME = "Echeance";
    @ManyToOne(fetch = FetchType.LAZY)
    private Abonnement abonnement;

    private LocalDate dateEcheance;

    @Column(precision = 19, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    private EcheanceStatut statut;

    private boolean notificationEnvoyee;

    private boolean rappelEnvoye;

    private int nombreRelances;

    private LocalDate dernierRappel;

    public Abonnement getAbonnement() {
        return abonnement;
    }

    public void setAbonnement(Abonnement abonnement) {
        this.abonnement = abonnement;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public EcheanceStatut getStatut() {
        return statut;
    }

    public void setStatut(EcheanceStatut statut) {
        this.statut = statut;
    }

    public boolean isNotificationEnvoyee() {
        return notificationEnvoyee;
    }

    public void setNotificationEnvoyee(boolean notificationEnvoyee) {
        this.notificationEnvoyee = notificationEnvoyee;
    }

    public boolean isRappelEnvoye() {
        return rappelEnvoye;
    }

    public void setRappelEnvoye(boolean rappelEnvoye) {
        this.rappelEnvoye = rappelEnvoye;
    }

    public int getNombreRelances() {
        return nombreRelances;
    }

    public void setNombreRelances(int nombreRelances) {
        this.nombreRelances = nombreRelances;
    }

    public LocalDate getDernierRappel() {
        return dernierRappel;
    }

    public void setDernierRappel(LocalDate dernierRappel) {
        this.dernierRappel = dernierRappel;
    }
}