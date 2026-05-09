package org.store.vente.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;
import org.store.produit.domain.model.Product;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = LigneCommandeVente.TABLE_NAME)
public class LigneCommandeVente extends BaseEntity {
    public static final String TABLE_NAME = "ligne_commande_vente";

    @ManyToOne(fetch = FetchType.LAZY)
    private CommandeVente commande;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    private int quantite;

    @Column(precision = 19, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantTotal;
}
