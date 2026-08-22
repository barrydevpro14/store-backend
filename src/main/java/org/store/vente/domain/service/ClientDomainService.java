package org.store.vente.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.service.GlobalService;
import org.store.common.tools.DateHelper;
import org.store.common.tools.LikePatternHelper;
import org.store.magasin.domain.model.Magasin;
import org.store.vente.application.dto.ClientFilter;
import org.store.vente.application.dto.ClientRequest;
import org.store.vente.application.dto.ClientResponse;
import org.store.vente.application.dto.ClientSummaryResponse;
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

    public long countByEntrepriseId(UUID entrepriseId) {
        return repository.countByEntrepriseId(entrepriseId);
    }

    /** Vérifie que email/telephone (si renseignés) ne sont pas déjà utilisés par un autre client de la même entreprise. */
    public void ensureOptionalContactsInEntreprise(String email, String telephone, UUID entrepriseId) {
        if (StringUtils.hasText(email) && repository.existsByEmailAndMagasinEntrepriseId(email, entrepriseId)) {
            throw new UniqueResourceException("utilisateur.email.alreadyExists", email);
        }
        if (StringUtils.hasText(telephone) && repository.existsByTelephoneAndMagasinEntrepriseId(telephone, entrepriseId)) {
            throw new UniqueResourceException("utilisateur.telephone.alreadyExists", telephone);
        }
    }

    /** Variante update : exclut le client courant de la vérification. */
    public void ensureOptionalContactsForUpdateInEntreprise(String email, String telephone, UUID entrepriseId, UUID excludeId) {
        if (StringUtils.hasText(email) && repository.existsByEmailAndMagasinEntrepriseIdAndIdNot(email, entrepriseId, excludeId)) {
            throw new UniqueResourceException("utilisateur.email.alreadyExists", email);
        }
        if (StringUtils.hasText(telephone) && repository.existsByTelephoneAndMagasinEntrepriseIdAndIdNot(telephone, entrepriseId, excludeId)) {
            throw new UniqueResourceException("utilisateur.telephone.alreadyExists", telephone);
        }
    }

    /** Recherche paginée pour sélecteur, scopée magasin. */
    public Page<ClientSummaryResponse> searchSummariesByMagasinId(UUID magasinId, String q, Pageable pageable) {
        return repository.searchSummaries(magasinId, null, LikePatternHelper.toLikePattern(q), pageable);
    }

    /** Recherche paginée pour sélecteur, scopée entreprise. */
    public Page<ClientSummaryResponse> searchSummariesByEntrepriseId(UUID entrepriseId, String q, Pageable pageable) {
        return repository.searchSummaries(null, entrepriseId, LikePatternHelper.toLikePattern(q), pageable);
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
