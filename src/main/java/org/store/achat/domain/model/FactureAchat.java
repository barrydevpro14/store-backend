package org.store.achat.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = FactureAchat.TABLE_NAME)
public class FactureAchat extends AuditableEntity {
    public static final String TABLE_NAME = "facture_achat";

    @Column(unique = true)
    private String numero;

    @Enumerated(EnumType.STRING)
    private StatutFacture statut;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantTotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @OneToOne(fetch = FetchType.LAZY)
    private CommandeAchat commande;

    @OneToMany(mappedBy = "facture")
    private List<PaiementAchat> paiements;

    private LocalDate date;

    private LocalDate dateEcheance;
}
