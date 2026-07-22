package org.store.vente.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.vente.application.dto.ClientFilter;
import org.store.vente.application.dto.ClientRequest;
import org.store.vente.application.dto.ClientResponse;
import org.store.vente.application.dto.ClientSummaryResponse;
import org.store.vente.domain.model.Client;

import java.util.UUID;

public interface IClientService {

    /**
     * Création d'un client rattaché à un magasin accessible par le caller.
     */
    ClientResponse create(ClientRequest clientRequest);

    /**
     * Lecture interne par id (utilisée par d'autres agrégats).
     */
    Client findById(UUID id);

    /**
     * Lecture par id, scopée sur le magasin/l'entreprise du caller.
     */
    ClientResponse findResponseById(UUID id);

    /**
     * Listing paginé des clients accessibles par le caller, avec filtres optionnels nom/prénom.
     * Scope magasin pour un employé, entreprise pour un propriétaire.
     */
    Page<ClientResponse> findAllForCurrentUser(ClientFilter clientFilter);

    /**
     * Modification d'un client accessible par le caller.
     */
    ClientResponse update(UUID id, ClientRequest clientRequest);

    /**
     * Suppression d'un client accessible par le caller.
     */
    void delete(UUID id);

    /**
     * Recherche paginée de clients pour les sélecteurs. Le terme {@code q} est cherché
     * dans le nom, prénom et téléphone. Scopée magasin pour un employé, entreprise pour
     * un propriétaire ; un {@code magasinId} explicite restreint le scope du propriétaire.
     */
    Page<ClientSummaryResponse> search(String q, UUID magasinId, Pageable pageable);

    /**
     * Vérifie qu'un client est accessible par le caller (via son magasin). Throw `ForbiddenException("client.notOwned")` sinon.
     */
    Client ensureAccessibleByCurrentUser(Client client);
}
