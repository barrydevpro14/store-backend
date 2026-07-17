package org.store.catalogue.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.activite.domain.model.ActiviteEconomique;
import org.store.common.base.AuditableEntity;

@Getter
@Setter
@Entity
@Table(
        name = CatalogueProduit.TABLE_NAME,
        uniqueConstraints = @UniqueConstraint(
                name = "uq_catalogue_produit_ref_libelle",
                columnNames = {"activite_economique_id", "reference", "libelle"}
        )
)
public class CatalogueProduit extends AuditableEntity {

    public static final String TABLE_NAME = "catalogue_produit";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activite_economique_id", nullable = false)
    private ActiviteEconomique activiteEconomique;

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false)
    private String libelle;

    @Column
    private String categorie;

    @Column(length = 500)
    private String description;
}
