package org.store.achat.domain.model;

import jakarta.persistence.*;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table
public class CommandeAchat extends AuditableEntity {
    public static final String TABLE_NAME = "commande_achat";

    @Column(unique = true)
    private String reference;

    @Enumerated(EnumType.STRING)
    private CommandeAchatStatut statut;

    @ManyToOne(fetch = FetchType.LAZY)
    private Fournisseur fournisseur;

    @ManyToOne(fetch = FetchType.LAZY)
    private Magasin magasin;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL)
    private List<LigneCommandeAchat> lignes;

    private LocalDate date;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public CommandeAchatStatut getStatut() {
        return statut;
    }

    public void setStatut(CommandeAchatStatut statut) {
        this.statut = statut;
    }

    public Fournisseur getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public List<LigneCommandeAchat> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneCommandeAchat> lignes) {
        this.lignes = lignes;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
