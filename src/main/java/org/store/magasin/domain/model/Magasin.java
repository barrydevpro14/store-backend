package org.store.magasin.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.store.common.base.AuditableEntity;
import org.store.users.domain.model.Employe;
import org.store.vente.domain.model.Client;

import java.util.List;

@Entity
@Table(name = Magasin.TABLE_NAME)
public class Magasin extends AuditableEntity {
    public final static String TABLE_NAME = "magasin";

    private String nom;
    private String adresse;

    @ManyToOne
    private Entreprise entreprise;

    @OneToMany(mappedBy = "magasin")
    private List<Employe> employees;

    @OneToMany(mappedBy = "magasin")
    private List<Client> clients;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public List<Employe> getEmployees() {
        return employees;
    }

    public void setEmployees(List<Employe> employees) {
        this.employees = employees;
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }
}
