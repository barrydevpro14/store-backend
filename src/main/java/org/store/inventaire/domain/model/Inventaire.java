package org.store.inventaire.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.inventaire.domain.enums.InventaireStatut;
import org.store.magasin.domain.model.Magasin;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = Inventaire.TABLE_NAME)
public class Inventaire extends AuditableEntity {
    public static final String TABLE_NAME = "inventaire";

    @ManyToOne(fetch = FetchType.LAZY)
    private Magasin magasin;

    private LocalDate dateInventaire;

    @Enumerated(EnumType.STRING)
    private InventaireStatut statut;

    @OneToMany(mappedBy = "inventaire")
    private List<LigneInventaire> lignes;
}
