package org.store.inventaire.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.inventaire.domain.enums.InventaireStatut;
import org.store.inventaire.domain.enums.TypeInventaire;
import org.store.magasin.domain.model.Magasin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = Inventaire.TABLE_NAME)
public class Inventaire extends AuditableEntity {
    public static final String TABLE_NAME = "inventaire";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "magasin_id", nullable = false)
    private Magasin magasin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeInventaire type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventaireStatut statut;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @OneToMany(mappedBy = "inventaire", cascade = CascadeType.ALL)
    private List<LigneInventaire> lignes;

    @OneToOne(mappedBy = "inventaire", fetch = FetchType.LAZY)
    private RapportInventaire rapport;
}
