package org.store.depense.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = Depense.TABLE_NAME)
public class Depense extends AuditableEntity {
    public static final String TABLE_NAME = "depense";

    @ManyToOne(fetch = FetchType.LAZY)
    private Magasin magasin;

    @ManyToOne(fetch = FetchType.LAZY)
    private CategoryDepense category;

    private String libelle;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dateDepense;

    private LocalDate dateEcheance;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;
}
