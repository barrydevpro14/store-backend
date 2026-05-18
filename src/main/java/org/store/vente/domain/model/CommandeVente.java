package org.store.vente.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.enums.MotifAnnulationVente;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL)
    private List<LigneCommandeVente> lignes;

    @OneToOne(mappedBy = "commande", fetch = FetchType.LAZY)
    private FactureClient facture;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MotifAnnulationVente motifAnnulation;

    @Column(columnDefinition = "TEXT")
    private String commentaireAnnulation;

    private LocalDateTime dateAnnulation;
}
