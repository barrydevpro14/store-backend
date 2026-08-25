package org.store.plateforme.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;

@Getter
@Setter
@Entity
@Table(name = CategoryDepensePlateforme.TABLE_NAME, uniqueConstraints = @UniqueConstraint(name = "uk_category_depense_plateforme_nom", columnNames = {"nom"}))
public class CategoryDepensePlateforme extends AuditableEntity {
    public static final String TABLE_NAME = "category_depense_plateforme";

    private String nom;

    private String description;

    private boolean actif = true;
}
