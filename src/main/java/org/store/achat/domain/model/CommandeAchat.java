package org.store.achat.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;

import java.time.LocalDate;
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
}
