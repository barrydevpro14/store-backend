package org.store.abonnement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.abonnement.domain.enums.StatutPreuvePaiement;
import org.store.common.base.AuditableEntity;
import org.store.common.model.PieceJointe;
import org.store.paiement.domain.model.MoyenPaiement;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = PreuvePaiement.TABLE_NAME)
public class PreuvePaiement extends AuditableEntity {
    public static final String TABLE_NAME = "preuve_paiement";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paiement_abonnement_id", nullable = false)
    private PaiementAbonnement paiementAbonnement;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moyen_id", nullable = false)
    private MoyenPaiement moyen;

    @Column(name = "reference_transaction", nullable = false, length = 255)
    private String referenceTransaction;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "preuve_id")
    private PieceJointe preuve;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutPreuvePaiement statut = StatutPreuvePaiement.EN_ATTENTE_VALIDATION;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;
}
