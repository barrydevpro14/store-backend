package org.store.plateforme.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.country.domain.model.Country;
import org.store.paiement.domain.model.MoyenPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = DepensePlateforme.TABLE_NAME)
public class DepensePlateforme extends AuditableEntity {
    public static final String TABLE_NAME = "depense_plateforme";

    @ManyToOne(fetch = FetchType.LAZY)
    private CategoryDepensePlateforme category;

    private String libelle;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dateDepense;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moyen_paiement_id", nullable = false)
    private MoyenPaiement modePaiement;

    /** Nullable — null = global/shared cost, not attributable to one market. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Country country;

    private boolean actif = true;
}
