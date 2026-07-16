package org.store.activite.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;

@Getter
@Setter
@Entity
@Table(name = ActiviteEconomique.TABLE_NAME)
public class ActiviteEconomique extends AuditableEntity {

    public static final String TABLE_NAME = "activite_economique";

    @Column(nullable = false, length = 150, unique = true)
    private String libelle;

    @Column(length = 500)
    private String description;
}
