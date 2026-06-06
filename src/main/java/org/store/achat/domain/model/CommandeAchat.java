package org.store.achat.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.MotifAnnulationAchat;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = CommandeAchat.TABLE_NAME)
public class CommandeAchat extends AuditableEntity {
    public static final String TABLE_NAME = "commande_achat";

    @Column(unique = true)
    private String reference;

    @Enumerated(EnumType.STRING)
    private CommandeAchatStatut statut;

    @ManyToOne(fetch = FetchType.LAZY)
    private Fournisseur fournisseur;

    @ManyToOne(fetch = FetchType.LAZY)
    private Magasin magasin;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL)
    private List<LigneCommandeAchat> lignes;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_annulation", length = 30)
    private MotifAnnulationAchat motifAnnulation;

    @Column(name = "commentaire_annulation", columnDefinition = "TEXT")
    private String commentaireAnnulation;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Column(name = "montant_total", precision = 19, scale = 2, nullable = false)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @OneToOne(mappedBy = "commande", fetch = FetchType.LAZY)
    private FactureAchat facture;
}
