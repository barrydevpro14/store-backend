package org.store.vente.domain.model;

import jakarta.persistence.*;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;
import org.store.users.domain.model.Employe;
import org.store.vente.domain.enums.CommandeVenteStatut;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Entity
@Table(name = CommandeVente.TABLE_NAME)
public class CommandeVente extends AuditableEntity {
    public static final String TABLE_NAME = "commande_vente";
    @Column(unique = true)
    private String reference;

    @Enumerated(EnumType.STRING)
    private CommandeVenteStatut statut;

    @ManyToOne(fetch = FetchType.LAZY)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    private Magasin magasin;

    @ManyToOne(fetch = FetchType.LAZY)
    private Employe vendeur;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL)
    private List<LigneCommandeVente> lignes;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantTotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantPaye;

    private LocalDate date;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public CommandeVenteStatut getStatut() {
        return statut;
    }

    public void setStatut(CommandeVenteStatut statut) {
        this.statut = statut;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public Employe getVendeur() {
        return vendeur;
    }

    public void setVendeur(Employe vendeur) {
        this.vendeur = vendeur;
    }

    public List<LigneCommandeVente> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneCommandeVente> lignes) {
        this.lignes = lignes;
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
}
