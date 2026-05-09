package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = TypeAbonnement.TABLE_NAME)
public class TypeAbonnement extends AuditableEntity {
    public static final String TABLE_NAME = "type_abonnement";

    @Column(nullable = false, unique = true)
    private String nom;

    private int dureeMois;

    @Enumerated(EnumType.STRING)
    private ReductionType reductionType;

    @Column(precision = 19, scale = 2)
    private BigDecimal valeurReduction;

    private boolean recommande = false;

    private boolean actif = true;

    private int ordre;
}
