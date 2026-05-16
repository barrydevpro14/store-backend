package org.store.inventaire.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;
import org.store.produit.domain.model.ProductFournisseur;

@Getter
@Setter
@Entity
@Table(name = LigneInventaire.TABLE_NAME)
public class LigneInventaire extends BaseEntity {
    public static final String TABLE_NAME = "ligne_inventaire";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventaire_id", nullable = false)
    private Inventaire inventaire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_fournisseur_id", nullable = false)
    private ProductFournisseur productFournisseur;

    @Column(name = "quantite_theorique", nullable = false)
    private int quantiteTheorique;

    @Column(name = "quantite_reelle", nullable = false)
    private int quantiteReelle;

    @Column(nullable = false)
    private int ecart;
}
