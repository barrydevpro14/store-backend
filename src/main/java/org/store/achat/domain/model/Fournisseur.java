package org.store.achat.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.model.Person;
import org.store.entreprise.domain.model.Entreprise;

@Getter
@Setter
@Entity
@Table(name = Fournisseur.TABLE_NAME)
public class Fournisseur extends Person {
    public final static String TABLE_NAME = "fournisseur";

    private String reference;

    private String origine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;
}
