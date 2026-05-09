package org.store.inventaire.domain.model;

import jakarta.persistence.*;
import org.store.common.base.AuditableEntity;
import org.store.inventaire.domain.enums.InventaireStatut;
import org.store.magasin.domain.model.Magasin;

import java.time.LocalDate;
import java.util.List;
@Entity
@Table(name = Inventaire.TABLE_NAME)
public class Inventaire extends AuditableEntity {
    public static final String TABLE_NAME = "inventaire";
    @ManyToOne(fetch = FetchType.LAZY)
    private Magasin magasin;

    private LocalDate dateInventaire;

    @Enumerated(EnumType.STRING)
    private InventaireStatut statut;

    @OneToMany(mappedBy = "inventaire")
    private List<LigneInventaire> lignes;

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public LocalDate getDateInventaire() {
        return dateInventaire;
    }

    public void setDateInventaire(LocalDate dateInventaire) {
        this.dateInventaire = dateInventaire;
    }

    public InventaireStatut getStatut() {
        return statut;
    }

    public void setStatut(InventaireStatut statut) {
        this.statut = statut;
    }

    public List<LigneInventaire> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneInventaire> lignes) {
        this.lignes = lignes;
    }
}
