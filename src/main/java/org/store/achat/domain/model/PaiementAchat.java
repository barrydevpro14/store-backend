package org.store.achat.domain.model;

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
@Table(name = PaiementAchat.TABLE_NAME)
public class PaiementAchat extends AuditableEntity {
    public static final String TABLE_NAME = "paiement_achat";

    @Column(precision = 19, scale = 2)
    private BigDecimal montant;

    private LocalDate datePaiement;

    @Enumerated(EnumType.STRING)
    private MoyenPaiement moyen;

    @ManyToOne(fetch = FetchType.LAZY)
    private FactureAchat facture;
}
