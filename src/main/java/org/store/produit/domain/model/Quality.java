package org.store.produit.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.store.common.base.BaseEntity;
@Entity
@Table(name = Quality.TABLE_NAME)
public class Quality extends BaseEntity {
    public static final String TABLE_NAME = "quality";
    private String libelle;
    private String description;

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
