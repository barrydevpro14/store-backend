package org.store.magasin.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.entreprise.domain.model.Entreprise;
import org.store.users.domain.model.Employe;
import org.store.vente.domain.model.Client;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = Magasin.TABLE_NAME)
public class Magasin extends AuditableEntity {
    public final static String TABLE_NAME = "magasin";

    private String nom;
    private String adresse;
    private boolean actif = true;

    @ManyToOne
    private Entreprise entreprise;

    @OneToMany(mappedBy = "magasin")
    private List<Employe> employees;

    @OneToMany(mappedBy = "magasin")
    private List<Client> clients;
}
