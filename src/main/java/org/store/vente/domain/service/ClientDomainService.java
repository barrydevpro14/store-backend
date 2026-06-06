package org.store.vente.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.common.tools.DateHelper;
import org.store.common.tools.LikePatternHelper;
import org.store.magasin.domain.model.Magasin;
import org.store.vente.application.dto.ClientFilter;
import org.store.vente.application.dto.ClientRequest;
import org.store.vente.application.dto.ClientResponse;
import org.store.vente.domain.model.Client;
import org.store.vente.domain.repository.ClientRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ClientDomainService extends GlobalService<Client, ClientRepository> {

    public ClientDomainService(ClientRepository repository) {
        super(repository);
    }

    public Client create(ClientRequest clientRequest, Magasin magasin) {
        Client client = new Client();
        client.setNom(clientRequest.nom());
        client.setPrenom(clientRequest.prenom());
        client.setEmail(clientRequest.email());
        client.setTelephone(clientRequest.telephone());
        client.setAdresse(clientRequest.adresse());
        client.setMagasin(magasin);
        return save(client);
    }

    public Page<ClientResponse> findResponsesByMagasinId(UUID magasinId, ClientFilter filter) {
        return repository.findResponsesByMagasinId(
                magasinId,
                LikePatternHelper.toLikePattern(filter.nom()),
                LikePatternHelper.toLikePattern(filter.prenom()),
                createdStart(filter),
                createdEnd(filter),
                filter.toPageable());
    }

    public Page<ClientResponse> findResponsesByEntrepriseId(UUID entrepriseId, ClientFilter filter) {
        return repository.findResponsesByEntrepriseId(
                entrepriseId,
                LikePatternHelper.toLikePattern(filter.nom()),
                LikePatternHelper.toLikePattern(filter.prenom()),
                createdStart(filter),
                createdEnd(filter),
                filter.toPageable());
    }

    private static LocalDateTime createdStart(ClientFilter filter) {
        return DateHelper.coalesceStart(filter.createdStartDateTime());
    }

    private static LocalDateTime createdEnd(ClientFilter filter) {
        return DateHelper.coalesceEnd(filter.createdEndDateTime());
    }
}
