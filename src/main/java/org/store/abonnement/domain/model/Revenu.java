package org.store.abonnement.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.country.domain.model.Country;
import org.store.entreprise.domain.model.Entreprise;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One row per validated subscription payment — the sole source of truth for platform revenue. */
@Getter
@Setter
@Entity
@Table(name = Revenu.TABLE_NAME)
public class Revenu extends AuditableEntity {
    public static final String TABLE_NAME = "revenu";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Country country;

    private BigDecimal montant;

    private LocalDate datePaiement;
}
