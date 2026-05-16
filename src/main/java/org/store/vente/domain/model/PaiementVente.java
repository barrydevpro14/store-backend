package org.store.vente.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = PaiementVente.TABLE_NAME)
public class PaiementVente extends AuditableEntity {
    public static final String TABLE_NAME = "paiement_vente";

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_paiement", nullable = false)
    private LocalDate datePaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MoyenPaiement moyen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facture_id", nullable = false)
    private FactureClient facture;
}
