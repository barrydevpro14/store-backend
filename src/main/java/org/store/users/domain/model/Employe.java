package org.store.users.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.store.magasin.domain.model.Magasin;

@Entity
@Table(name = Employe.TABLE_NAME)
public class Employe extends Utilisateur {
    public final static String TABLE_NAME = "employees";

    @ManyToOne
    private Magasin magasin;

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }
}
