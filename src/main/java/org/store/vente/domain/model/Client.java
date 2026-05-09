package org.store.vente.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.store.common.model.Person;
import org.store.magasin.domain.model.Magasin;

@Entity
@Table(name = Client.TABLE_NAME)
public class Client extends Person {
    public final static String TABLE_NAME = "client";
    @ManyToOne(fetch = FetchType.LAZY)
    private Magasin magasin;

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }
}
