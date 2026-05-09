package org.store.depense.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;

@Getter
@Setter
@Entity
@Table(name = CategoryDepense.TABLE_NAME)
public class CategoryDepense extends AuditableEntity {
    public static final String TABLE_NAME = "category_depense";

    @Column(nullable = false, unique = true)
    private String nom;

    private String description;

    private boolean actif = true;
}
