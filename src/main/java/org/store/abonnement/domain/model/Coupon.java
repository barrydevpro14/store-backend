package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = Coupon.TABLE_NAME)
public class Coupon extends AuditableEntity {
    public static final String TABLE_NAME = "coupon";

    @Column(nullable = false, unique = true)
    private String code;

    private String description;

    @Enumerated(EnumType.STRING)
    private ReductionType reductionType;

    @Column(precision = 19, scale = 2)
    private BigDecimal valeurReduction;

    private int nombreUtilisationsMax;

    private int nombreUtilisations = 0;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    private PlanAbonnement plan;
}
