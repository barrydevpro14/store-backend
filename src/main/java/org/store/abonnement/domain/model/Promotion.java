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
@Table(name = Promotion.TABLE_NAME)
public class Promotion extends AuditableEntity {
    public static final String TABLE_NAME = "promotion";

    private String nom;

    private String description;

    @Enumerated(EnumType.STRING)
    private ReductionType reductionType;

    @Column(precision = 19, scale = 2)
    private BigDecimal valeurReduction;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    private PlanAbonnement plan;
}
