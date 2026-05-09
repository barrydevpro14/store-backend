package org.store.achat.domain.model;

import jakarta.persistence.*;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = FactureAchat.TABLE_NAME)
public class FactureAchat extends AuditableEntity {
    public static final String TABLE_NAME = "facture_achat";

    @Column(unique = true)
    private String numero;

    @Enumerated(EnumType.STRING)
    private StatutFacture statut;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantTotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @OneToOne(fetch = FetchType.LAZY)
    private CommandeAchat commande;

    @OneToMany(mappedBy = "facture")
    private List<PaiementAchat> paiements;

    private LocalDate date;

    private LocalDate dateEcheance;

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public StatutFacture getStatut() {
        return statut;
    }

    public void setStatut(StatutFacture statut) {
        this.statut = statut;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(BigDecimal montantTotal) {
        this.montantTotal = montantTotal;
    }

    public CommandeAchat getCommande() {
        return commande;
    }

    public void setCommande(CommandeAchat commande) {
        this.commande = commande;
    }

    public BigDecimal getMontantPaye() {
        return montantPaye;
    }

    public void setMontantPaye(BigDecimal montantPaye) {
        this.montantPaye = montantPaye;
    }

    public List<PaiementAchat> getPaiements() {
        return paiements;
    }

    public void setPaiements(List<PaiementAchat> paiements) {
        this.paiements = paiements;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }
}
