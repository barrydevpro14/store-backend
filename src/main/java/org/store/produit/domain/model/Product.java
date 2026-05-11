package org.store.produit.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.common.model.PieceJointe;
import org.store.entreprise.domain.model.Entreprise;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = Product.TABLE_NAME)
public class Product extends AuditableEntity {
    public static final String TABLE_NAME = "product";

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String reference;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    private CategoryProduct categoryProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    private Quality quality;

    @ManyToOne(fetch = FetchType.LAZY)
    private Entreprise entreprise;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "product_id")
    private List<PieceJointe> images = new ArrayList<>();

    public void setImages(List<PieceJointe> images) {
        this.images.clear();
        this.images.addAll(images);
    }
}
