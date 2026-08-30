package org.store.paiement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.country.domain.model.Country;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = Facturation.TABLE_NAME)
public class Facturation extends AuditableEntity {
    public static final String TABLE_NAME = "facturation";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moyen_paiement_id", nullable = false)
    private MoyenPaiement moyenPaiement;

    /** Empty set means the billing number is global (available in every country). */
    @ManyToMany
    @JoinTable(
            name = "facturation_pays",
            joinColumns = @JoinColumn(name = "facturation_id"),
            inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    private Set<Country> pays = new HashSet<>();

    @Column(name = "numero_facturation", nullable = false, length = 100)
    private String numeroFacturation;

    @Column(nullable = false)
    private boolean actif = true;
}
