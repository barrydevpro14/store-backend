package org.store.achat.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.store.common.model.Person;

@Entity
@Table(name = Fournisseur.TABLE_NAME)
public class Fournisseur extends Person {
    public final static String TABLE_NAME = "fournisseur";
    private String reference;
    private String origine;
}
