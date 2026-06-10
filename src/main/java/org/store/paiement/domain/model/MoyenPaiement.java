package org.store.paiement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;

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
}
