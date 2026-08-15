package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(
        name = PlanAbonnementTarif.TABLE_NAME,
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_plan_tarif_periodicite", columnNames = {"plan_id", "periodicite"})
        }
)
public class PlanAbonnementTarif extends AuditableEntity {

    public static final String TABLE_NAME = "plan_abonnement_tarif";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanAbonnement plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PeriodiciteAbonnement periodicite;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal prix;

    private boolean actif = true;

    private boolean recommande = false;

    private Integer ordre;
}
