package org.store.depense.domain.model;

import jakarta.persistence.*;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;

import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Table(name = Depense.TABLE_NAME)
public class Depense extends AuditableEntity {
    public static final String TABLE_NAME = "depense";
    @ManyToOne(fetch = FetchType.LAZY)
    private Magasin magasin;

    @ManyToOne(fetch = FetchType.LAZY)
    private CategoryDepense category;

    private String libelle;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dateDepense;

    private LocalDate dateEcheance;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;


    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public CategoryDepense getCategory() {
        return category;
    }

    public void setCategory(CategoryDepense category) {
        this.category = category;
    }

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

    public LocalDate getDateDepense() {
        return dateDepense;
    }

    public void setDateDepense(LocalDate dateDepense) {
        this.dateDepense = dateDepense;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }
}
