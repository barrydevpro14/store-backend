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
@Table(name = MoyenPaiement.TABLE_NAME)
public class MoyenPaiement extends AuditableEntity {
    public static final String TABLE_NAME = "moyen_paiement";

    @Column(nullable = false, length = 100)
    private String libelle;

    /** Code interne (CASH, WAVE, OM, CARD) — utilisé pour le seed idempotent uniquement. */
    @Column(nullable = false, length = 20, unique = true, updatable = false)
    private String code;

    @Column(nullable = false)
    private boolean actif = true;

    @ManyToMany
    @JoinTable(
            name = "moyen_paiement_pays",
            joinColumns = @JoinColumn(name = "moyen_paiement_id"),
            inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    private Set<Country> pays = new HashSet<>();
}
