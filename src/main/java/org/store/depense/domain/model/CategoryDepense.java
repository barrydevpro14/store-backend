package org.store.depense.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.entreprise.domain.model.Entreprise;

@Getter
@Setter
@Entity
@Table(name = CategoryDepense.TABLE_NAME, uniqueConstraints = @UniqueConstraint(name = "uk_category_depense_entreprise_nom", columnNames = {"entreprise_id", "nom"}))
public class CategoryDepense extends AuditableEntity {
    public static final String TABLE_NAME = "category_depense";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Entreprise entreprise;

    private String nom;

    private String description;

    private boolean actif = true;
}
