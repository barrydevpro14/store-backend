package org.store.vente.application.dto;

import org.store.common.tools.NameHelper;
import org.store.vente.domain.model.Client;

import java.util.UUID;

public record ClientSummaryResponse(
        UUID id,
        String nomComplet,
        String telephone
) {
    public ClientSummaryResponse(Client client) {
        this(client.getId(),
                NameHelper.formatNomComplet(client.getNom(), client.getPrenom()),
                client.getTelephone());
    }
}
