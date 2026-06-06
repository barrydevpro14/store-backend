package org.store.vente.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.ClientFilter;
import org.store.vente.application.dto.ClientRequest;
import org.store.vente.application.dto.ClientResponse;
import org.store.vente.application.service.IClientService;
import org.store.vente.domain.model.Client;
import org.store.vente.domain.service.ClientDomainService;

import java.util.UUID;

/**
 * Gère le CRUD des clients, scopé sur le magasin (employé) ou l'entreprise (propriétaire) du caller.
 */
@Service
public class ClientServiceImpl implements IClientService {

    private final ClientDomainService clientDomainService;
    private final IMagasinService magasinService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public ClientServiceImpl(ClientDomainService clientDomainService,
                             IMagasinService magasinService,
                             ICurrentUserService currentUserService,
                             ValidatorService validatorService) {
        this.clientDomainService = clientDomainService;
        this.magasinService = magasinService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Crée un client après contrôle d'accès du caller au magasin cible. */
    @Override
    @Transactional
    public ClientResponse create(ClientRequest clientRequest) {
        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(clientRequest.magasinId()));
        return new ClientResponse(clientDomainService.create(clientRequest, magasin));
    }

    /** Retourne le client ou lève `EntityException`. */
    @Override
    public Client findById(UUID id) {
        return clientDomainService.findById(id);
    }

    /** Retourne le client en `Response` après vérification de l'accès du caller. */
    @Override
    public ClientResponse findResponseById(UUID id) {
        Client client = ensureAccessibleByCurrentUser(clientDomainService.findById(id));
        return new ClientResponse(client);
    }

    /** Liste paginée scopée magasin (si caller employé) ou entreprise (si caller propriétaire/admin), filtrée par nom/prénom optionnels. */
    @Override
    public Page<ClientResponse> findAllForCurrentUser(ClientFilter clientFilter) {
        validatorService.validate(clientFilter);
        UserPrincipal currentUser = currentUserService.getCurrent();

        if (currentUser.magasinId() != null) {
            return clientDomainService.findResponsesByMagasinId(currentUser.magasinId(), clientFilter);
        }

        return clientDomainService.findResponsesByEntrepriseId(currentUser.entrepriseId(), clientFilter);
    }

    /**
     * Met à jour un client après contrôle d'accès du caller, et applique éventuellement un changement de magasin
     * (le magasin cible doit lui aussi être accessible par le caller).
     */
    @Override
    @Transactional
    public ClientResponse update(UUID id, ClientRequest clientRequest) {
        Client client = ensureAccessibleByCurrentUser(clientDomainService.findById(id));

        Magasin magasin = client.getMagasin();
        if (!magasin.getId().equals(clientRequest.magasinId())) {
            magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(clientRequest.magasinId()));
        }

        client.setNom(clientRequest.nom());
        client.setPrenom(clientRequest.prenom());
        client.setEmail(clientRequest.email());
        client.setTelephone(clientRequest.telephone());
        client.setAdresse(clientRequest.adresse());
        client.setMagasin(magasin);

        return new ClientResponse(clientDomainService.save(client));
    }

    /** Supprime un client après contrôle d'accès du caller. */
    @Override
    @Transactional
    public void delete(UUID id) {
        Client client = ensureAccessibleByCurrentUser(clientDomainService.findById(id));
        clientDomainService.delete(client);
    }

    /**
     * Lève `ForbiddenException` si le client n'est pas accessible par le caller : un employé n'accède qu'aux clients
     * de son magasin, un propriétaire à ceux de toute son entreprise.
     */
    @Override
    public Client ensureAccessibleByCurrentUser(Client client) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        Magasin magasin = client.getMagasin();

        if (currentUser.magasinId() != null) {
            if (!magasin.getId().equals(currentUser.magasinId())) {
                throw new ForbiddenException("client.notOwned");
            }
            return client;
        }

        if (!magasin.getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("client.notOwned");
        }
        return client;
    }
}
