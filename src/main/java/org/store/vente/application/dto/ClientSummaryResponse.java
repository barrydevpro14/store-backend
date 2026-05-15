package org.store.vente.application.dto;

import org.store.vente.domain.model.Client;

import java.util.UUID;

public record ClientSummaryResponse(
        UUID id,
        String nomComplet
) {
    public ClientSummaryResponse(Client client) {
        this(client.getId(), buildNomComplet(client));
    }

    private static String buildNomComplet(Client client) {
        String prenom = client.getPrenom();
        if (prenom == null || prenom.isBlank()) {
            return client.getNom();
        }
        return client.getNom() + " " + prenom;
    }
}
