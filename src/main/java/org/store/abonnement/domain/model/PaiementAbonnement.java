package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.common.base.AuditableEntity;
import org.store.common.model.PieceJointe;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = PaiementAbonnement.TABLE_NAME)
public class PaiementAbonnement extends AuditableEntity {
    public static final String TABLE_NAME = "paiement_abonnement";

    @ManyToOne(fetch = FetchType.LAZY)
    private Abonnement abonnement;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantAvantReduction;

    @Column(precision = 19, scale = 2)
    private BigDecimal reduction;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantFinal;

    private LocalDate datePaiement;

    @Enumerated(EnumType.STRING)
    private MoyenPaiement moyen;

    private String referenceTransaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutPaiementAbonnement statut = StatutPaiementAbonnement.EN_ATTENTE_VALIDATION;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "preuve_id")
    private PieceJointe preuve;

    @Column(columnDefinition = "TEXT")
    private String motifRejet;
}
