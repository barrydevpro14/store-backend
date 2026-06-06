package org.store.users.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.entreprise.domain.model.Entreprise;

@Getter
@Setter
@Entity
@Table(name = Proprietaire.TABLE_NAME)
public class Proprietaire extends Utilisateur {
    public static final String TABLE_NAME = "proprietaire";

    @OneToOne(mappedBy = "proprietaire")
    private Entreprise entreprise;
}
