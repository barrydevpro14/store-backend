package org.store.vente.application.dto;

import org.store.magasin.domain.model.Magasin;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.model.Client;

import java.time.LocalDate;

public record CommandeVenteCreate(
        Client client,
        Magasin magasin,
        LocalDate dateVente,
        String reference,
        CommandeVenteStatut statut
) {
}
