package org.store.achat.domain.model;

import jakarta.persistence.*;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Table(name = PaiementAchat.TABLE_NAME)
public class PaiementAchat extends AuditableEntity {
    public static final String TABLE_NAME = "paiement_achat";
    @Column(precision = 19, scale = 2)
    private BigDecimal montant;

    private LocalDate datePaiement;

    @Enumerated(EnumType.STRING)
    private MoyenPaiement moyen;

    @ManyToOne(fetch = FetchType.LAZY)
    private FactureAchat facture;

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
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

    public FactureAchat getFacture() {
        return facture;
    }

    public void setFacture(FactureAchat facture) {
        this.facture = facture;
    }
}
