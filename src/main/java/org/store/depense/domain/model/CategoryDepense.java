package org.store.depense.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.store.common.base.AuditableEntity;
@Entity
@Table(name = CategoryDepense.TABLE_NAME)
public class CategoryDepense extends AuditableEntity {
    public static final String TABLE_NAME = "category_depense";
    @Column(nullable = false, unique = true)
    private String nom;

    private String description;

    private boolean actif = true;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
