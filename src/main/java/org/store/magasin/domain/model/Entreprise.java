package org.store.magasin.domain.model;

import jakarta.persistence.*;
import org.store.abonnement.domain.model.Abonnement;
import org.store.common.base.AuditableEntity;
import org.store.produit.domain.model.Product;
import org.store.users.domain.model.Proprietaire;

import java.util.List;

@Entity
@Table(name = Entreprise.TABLE_NAME)
public class Entreprise extends AuditableEntity {
    public final static String TABLE_NAME = "entreprise";

    private String sigle;
    private String RaisonSociale;
    private String ninea;
    private String rccm;
    private String adresse;
    private boolean trialUsed = false;
    @OneToMany(mappedBy = "entreprise" , fetch = FetchType.LAZY , cascade = CascadeType.ALL)
    private List<Abonnement> abonnements;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private Proprietaire proprietaire;

    @OneToMany(mappedBy = "entreprise")
    private List<Magasin> magasins;

    @OneToMany(mappedBy = "entreprise")
    private List<Product> products;

    public String getSigle() {
        return sigle;
    }

    public void setSigle(String sigle) {
        this.sigle = sigle;
    }

    public String getRaisonSociale() {
        return RaisonSociale;
    }

    public void setRaisonSociale(String raisonSociale) {
        RaisonSociale = raisonSociale;
    }

    public String getNinea() {
        return ninea;
    }

    public void setNinea(String ninea) {
        this.ninea = ninea;
    }

    public String getRccm() {
        return rccm;
    }

    public void setRccm(String rccm) {
        this.rccm = rccm;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public Proprietaire getProprietaire() {
        return proprietaire;
    }

    public void setProprietaire(Proprietaire proprietaire) {
        this.proprietaire = proprietaire;
    }

    public List<Magasin> getMagasins() {
        return magasins;
    }

    public void setMagasins(List<Magasin> magasins) {
        this.magasins = magasins;
    }
}
