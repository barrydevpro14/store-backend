package org.store.achat.application.dto;

import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.model.Fournisseur;
import org.store.magasin.domain.model.Magasin;

import java.time.LocalDate;

public record CommandeAchatCreate(
        Fournisseur fournisseur,
        Magasin magasin,
        LocalDate dateCommande,
        String reference,
        CommandeAchatStatut statut
) {
}
