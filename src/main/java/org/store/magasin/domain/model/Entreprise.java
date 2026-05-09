package org.store.magasin.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.abonnement.domain.model.Abonnement;
import org.store.common.base.AuditableEntity;
import org.store.produit.domain.model.Product;
import org.store.users.domain.model.Proprietaire;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = Entreprise.TABLE_NAME)
public class Entreprise extends AuditableEntity {
    public final static String TABLE_NAME = "entreprise";

    private String sigle;
    private String RaisonSociale;
    private String ninea;
    private String rccm;
    private String adresse;
    private boolean trialUsed = false;

    @OneToMany(mappedBy = "entreprise", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Abonnement> abonnements;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private Proprietaire proprietaire;

    @OneToMany(mappedBy = "entreprise")
    private List<Magasin> magasins;

    @OneToMany(mappedBy = "entreprise")
    private List<Product> products;
}
