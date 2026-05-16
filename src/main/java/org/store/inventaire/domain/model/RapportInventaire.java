package org.store.inventaire.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.inventaire.domain.enums.StatutRapport;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = RapportInventaire.TABLE_NAME)
public class RapportInventaire extends AuditableEntity {
    public static final String TABLE_NAME = "rapport_inventaire";

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventaire_id", nullable = false, unique = true)
    private Inventaire inventaire;

    @Column(name = "montant_automatique", nullable = false, precision = 19, scale = 2)
    private BigDecimal montantAutomatique;

    @Column(name = "montant_physique", nullable = false, precision = 19, scale = 2)
    private BigDecimal montantPhysique;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal ecart;

    @Column(name = "montant_caisse", nullable = false, precision = 19, scale = 2)
    private BigDecimal montantCaisse;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal depense;

    @Column(name = "montant_roulement", nullable = false, precision = 19, scale = 2)
    private BigDecimal montantRoulement;

    @Column(name = "date_debut_periode", nullable = false)
    private LocalDate dateDebutPeriode;

    @Column(name = "date_fin_periode", nullable = false)
    private LocalDate dateFinPeriode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal benefice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutRapport status;
}
