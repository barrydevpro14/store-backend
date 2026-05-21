package org.store.abonnement.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = PlanAbonnement.TABLE_NAME)
public class PlanAbonnement extends AuditableEntity {
    public static final String TABLE_NAME = "plan_abonnement";

    @Column(nullable = false, unique = true)
    private String nom;

    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal prix;

    private int nombreMagasinsMax;

    private int nombreEmployesMax;

    private boolean gestionStock = true;

    private boolean gestionVente = true;

    private boolean gestionAchat = true;

    private boolean gestionComptabilite = false;

    private boolean actif = true;

    private boolean visible = true;


    private int ordre;
}
