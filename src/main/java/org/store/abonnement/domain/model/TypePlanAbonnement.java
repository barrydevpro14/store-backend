package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;

/**
 * Durée d'abonnement proposée pour un plan donné (Mensuel, Trimestriel,
 * Annuel, …). Chaque {@link PlanAbonnement} peut en avoir plusieurs ;
 * la durée + l'éventuelle réduction définissent la déclinaison
 * tarifaire choisie par le client à la souscription.
 *
 * Unicité scopée par plan via la contrainte SQL `uk_type_plan_plan_nom`
 * (plan_abonnement_id, nom) — "Mensuel" peut donc exister sur Pro ET
 * sur Premium sans collision.
 */
@Getter
@Setter
@Entity
@Table(name = TypePlanAbonnement.TABLE_NAME)
public class TypePlanAbonnement extends AuditableEntity {
    public static final String TABLE_NAME = "type_plan_abonnement";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_abonnement_id", nullable = false)
    private PlanAbonnement plan;

    @Column(nullable = false)
    private String nom;

    private int dureeMois;

    @Enumerated(EnumType.STRING)
    private ReductionType reductionType;

    @Column(precision = 19, scale = 2)
    private BigDecimal valeurReduction;

    private boolean recommande = false;

    private boolean actif = true;

    // Marqueur du type d'essai gratuit. Seul {@code true} pour le type
    // utilisé par {@code AbonnementServiceImpl.createTrialForSignup}
    // (Abonnement avec statut TRIAL au signup). Il existe au plus un type
    // {@code trial=true} actif en base.
    private boolean trial = false;

    private int ordre;
}
