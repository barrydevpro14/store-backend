package org.store.produit.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.BaseEntity;
import org.store.entreprise.domain.model.Entreprise;

@Getter
@Setter
@Entity
@Table(name = CategoryProduct.TABLE_NAME)
public class CategoryProduct extends BaseEntity {
    public static final String TABLE_NAME = "category_product";

    @Column(nullable = false)
    private String libelle;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;
}
