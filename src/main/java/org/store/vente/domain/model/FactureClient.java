package org.store.vente.domain.model;

import jakarta.persistence.*;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = FactureClient.TABLE_NAME)
public class FactureClient extends AuditableEntity {
    public static final String TABLE_NAME = "facture_client";

    @Column(unique = true)
    private String numero;

    @Enumerated(EnumType.STRING)
    private StatutFacture statut;

    @OneToOne(fetch = FetchType.LAZY)
    private CommandeVente commande;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantTotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantPaye;

    private LocalDate date;
    private LocalDate dateEcheache;

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

    public CommandeVente getCommande() {
        return commande;
    }

    public void setCommande(CommandeVente commande) {
        this.commande = commande;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(BigDecimal montantTotal) {
        this.montantTotal = montantTotal;
    }

    public BigDecimal getMontantPaye() {
        return montantPaye;
    }

    public void setMontantPaye(BigDecimal montantPaye) {
        this.montantPaye = montantPaye;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDateEcheache() {
        return dateEcheache;
    }

    public void setDateEcheache(LocalDate dateEcheache) {
        this.dateEcheache = dateEcheache;
    }
}
