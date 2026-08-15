package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = PlanAbonnement.TABLE_NAME)
public class PlanAbonnement extends AuditableEntity {
    public static final String TABLE_NAME = "plan_abonnement";

    @Column(nullable = false, unique = true)
    private String nom;

    private String description;

    private int nombreMagasinsMax;

    private int nombreEmployesMax;

    private boolean gestionStock = true;

    private boolean gestionVente = true;

    private boolean gestionAchat = true;

    private boolean gestionComptabilite = false;

    private boolean actif = true;

    private boolean visible = true;

    private boolean trial = false;

    private int ordre;

    @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY)
    private List<PlanAbonnementTarif> tarifs = new ArrayList<>();
}
