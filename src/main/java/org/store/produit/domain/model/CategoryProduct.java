package org.store.produit.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = CategoryProduct.TABLE_NAME)
public class CategoryProduct extends BaseEntity {
    public static final String TABLE_NAME = "category_product";
    private String libelle;
    private String description;
}
