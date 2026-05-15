package org.store.achat.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = LigneCommandeAchat.TABLE_NAME)
public class LigneCommandeAchat extends BaseEntity {
    public static final String TABLE_NAME = "ligne_commande_achat";

    @ManyToOne(fetch = FetchType.LAZY)
    private CommandeAchat commande;

    @ManyToOne(fetch = FetchType.LAZY)
    private ProductFournisseur productFournisseur;

    private int quantite;

    @Column(precision = 19, scale = 2)
    private BigDecimal prixAchat;

    @Column(name = "prix_vente", nullable = false, precision = 19, scale = 2)
    private BigDecimal prixVente;
}
