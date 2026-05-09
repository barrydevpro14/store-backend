package org.store.vente.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;
import org.store.users.domain.model.Employe;
import org.store.vente.domain.enums.CommandeVenteStatut;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = CommandeVente.TABLE_NAME)
public class CommandeVente extends AuditableEntity {
    public static final String TABLE_NAME = "commande_vente";

    @Column(unique = true)
    private String reference;

    @Enumerated(EnumType.STRING)
    private CommandeVenteStatut statut;

    @ManyToOne(fetch = FetchType.LAZY)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    private Magasin magasin;

    @ManyToOne(fetch = FetchType.LAZY)
    private Employe vendeur;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL)
    private List<LigneCommandeVente> lignes;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantTotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal montantPaye;

    private LocalDate date;
}
