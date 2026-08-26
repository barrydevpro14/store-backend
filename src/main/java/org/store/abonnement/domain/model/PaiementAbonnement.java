package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = PaiementAbonnement.TABLE_NAME)
public class PaiementAbonnement extends AuditableEntity {
    public static final String TABLE_NAME = "paiement_abonnement";

    @ManyToOne(fetch = FetchType.LAZY)
    private Abonnement abonnement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_abonnement_tarif_id")
    private PlanAbonnementTarif planAbonnementTarif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantAvantReduction;

    @Column(precision = 19, scale = 2)
    private BigDecimal reduction;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantFinal;

    private LocalDate datePaiement;

    private LocalDate dateEcheance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutPaiementAbonnement statut = StatutPaiementAbonnement.FACTURE_GENEREE;

    @OneToMany(mappedBy = "paiementAbonnement", fetch = FetchType.LAZY)
    private List<PreuvePaiement> preuves = new ArrayList<>();
}
