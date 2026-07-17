package org.store.catalogue.application.dto;

import org.store.catalogue.domain.model.CatalogueProduit;

import java.util.UUID;

public record CatalogueProduitResponse(
        UUID id,
        String reference,
        String libelle,
        String categorie,
        String description,
        UUID activiteEconomiqueId,
        String activiteEconomiqueLibelle
) {
    public CatalogueProduitResponse(CatalogueProduit c) {
        this(
                c.getId(),
                c.getReference(),
                c.getLibelle(),
                c.getCategorie(),
                c.getDescription(),
                c.getActiviteEconomique().getId(),
                c.getActiviteEconomique().getLibelle()
        );
    }
}
