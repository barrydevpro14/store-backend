package org.store.users.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.store.magasin.domain.model.Entreprise;

@Entity
@Table(name = Proprietaire.TABLE_NAME)
public class Proprietaire extends Utilisateur {
    public static final String TABLE_NAME = "proprietaire";

    @OneToOne(mappedBy = "proprietaire")
    private Entreprise entreprise;

    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }
}
