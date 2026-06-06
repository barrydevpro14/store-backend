package org.store.vente.application.dto;

import org.store.vente.domain.model.Client;

import java.util.UUID;

public record ClientResponse(
        UUID id,
        String nom,
        String prenom,
        String email,
        String telephone,
        String adresse
) {
    public ClientResponse(Client client) {
        this(
                client.getId(),
                client.getNom(),
                client.getPrenom(),
                client.getEmail(),
                client.getTelephone(),
                client.getAdresse()
        );
    }
}
