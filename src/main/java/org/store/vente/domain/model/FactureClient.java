package org.store.vente.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.base.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = FactureClient.TABLE_NAME)
public class FactureClient extends AuditableEntity {
    public static final String TABLE_NAME = "facture_client";

    @Column(unique = true)
    private String numero;

    @Enumerated(EnumType.STRING)
    private StatutFacture statut;

    @OneToOne(fetch = FetchType.LAZY)
    private CommandeVente commande;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantTotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantPaye;

    private LocalDate date;

    @Column(nullable = false)
    private LocalDate dateEcheance;
}
