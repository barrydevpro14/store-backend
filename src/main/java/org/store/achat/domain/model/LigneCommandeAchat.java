package org.store.achat.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Column(name = "quantite_recue", nullable = false)
    private int quantiteRecue;

    @Column(precision = 19, scale = 2)
    private BigDecimal prixAchat;

    @Column(name = "prix_vente", nullable = false, precision = 19, scale = 2)
    private BigDecimal prixVente;

    @Column(name = "numero_lot", length = 100)
    private String numeroLot;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;
}
