package org.store.paiement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.country.domain.model.Country;

@Getter
@Setter
@Entity
@Table(name = Facturation.TABLE_NAME)
public class Facturation extends AuditableEntity {
    public static final String TABLE_NAME = "facturation";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moyen_paiement_id", nullable = false)
    private MoyenPaiement moyenPaiement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pays_id")
    private Country pays;

    @Column(name = "numero_facturation", nullable = false, length = 100)
    private String numeroFacturation;

    @Column(nullable = false)
    private boolean actif = true;
}
