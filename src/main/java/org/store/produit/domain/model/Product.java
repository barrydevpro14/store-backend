package org.store.produit.domain.model;

import jakarta.persistence.*;
import org.store.common.base.AuditableEntity;
import org.store.common.model.PieceJointe;
import org.store.magasin.domain.model.Entreprise;

import java.util.ArrayList;
import java.util.List;

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
    @OneToMany(fetch = FetchType.LAZY , cascade = CascadeType.ALL , orphanRemoval = true)
    @JoinColumn(name = "product_id")
    private List<PieceJointe> images = new ArrayList<>();

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CategoryProduct getCategoryProduct() {
        return categoryProduct;
    }

    public void setCategoryProduct(CategoryProduct categoryProduct) {
        this.categoryProduct = categoryProduct;
    }

    public Quality getQuality() {
        return quality;
    }

    public void setQuality(Quality quality) {
        this.quality = quality;
    }

    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }

    public List<PieceJointe> getImages() {
        return images;
    }

    public void setImages(List<PieceJointe> images) {
        this.images.clear();
        this.images.addAll(images);
    }
}
