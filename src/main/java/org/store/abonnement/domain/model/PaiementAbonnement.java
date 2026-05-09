package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Table(name = PaiementAbonnement.TABLE_NAME)
public class PaiementAbonnement extends AuditableEntity {
    public static final String TABLE_NAME = "paiement_abonnement";
    @ManyToOne(fetch = FetchType.LAZY)
    private Abonnement abonnement;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantAvantReduction;

    @Column(precision = 19, scale = 2)
    private BigDecimal reduction;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantFinal;

    private LocalDate datePaiement;

    @Enumerated(EnumType.STRING)
    private MoyenPaiement moyen;

    private String referenceTransaction;

    public Abonnement getAbonnement() {
        return abonnement;
    }

    public void setAbonnement(Abonnement abonnement) {
        this.abonnement = abonnement;
    }

    public BigDecimal getMontantAvantReduction() {
        return montantAvantReduction;
    }

    public void setMontantAvantReduction(BigDecimal montantAvantReduction) {
        this.montantAvantReduction = montantAvantReduction;
    }

    public BigDecimal getReduction() {
        return reduction;
    }

    public void setReduction(BigDecimal reduction) {
        this.reduction = reduction;
    }

    public BigDecimal getMontantFinal() {
        return montantFinal;
    }

    public void setMontantFinal(BigDecimal montantFinal) {
        this.montantFinal = montantFinal;
    }

    public LocalDate getDatePaiement() {
        return datePaiement;
    }

    public void setDatePaiement(LocalDate datePaiement) {
        this.datePaiement = datePaiement;
    }

    public MoyenPaiement getMoyen() {
        return moyen;
    }

    public void setMoyen(MoyenPaiement moyen) {
        this.moyen = moyen;
    }

    public String getReferenceTransaction() {
        return referenceTransaction;
    }

    public void setReferenceTransaction(String referenceTransaction) {
        this.referenceTransaction = referenceTransaction;
    }
}
