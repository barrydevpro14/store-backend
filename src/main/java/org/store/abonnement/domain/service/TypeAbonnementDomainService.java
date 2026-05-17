package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.domain.model.TypeAbonnement;
import org.store.abonnement.domain.repository.TypeAbonnementRepository;
import org.store.common.service.GlobalService;

import java.util.List;

@Service
public class TypeAbonnementDomainService extends GlobalService<TypeAbonnement, TypeAbonnementRepository> {
    public TypeAbonnementDomainService(TypeAbonnementRepository repository) {
        super(repository);
    }

    public TypeAbonnement create(SubscriptionTypeRequest subscriptionTypeRequest) {
        TypeAbonnement type = new TypeAbonnement();
        applyRequest(type, subscriptionTypeRequest);
        return save(type);
    }

    public TypeAbonnement applyRequest(TypeAbonnement type, SubscriptionTypeRequest subscriptionTypeRequest) {
        type.setNom(subscriptionTypeRequest.nom());
        type.setDureeMois(subscriptionTypeRequest.dureeMois());
        type.setReductionType(subscriptionTypeRequest.reductionTypeAsEnum());
        type.setValeurReduction(subscriptionTypeRequest.valeurReduction());
        type.setRecommande(subscriptionTypeRequest.recommande());
        type.setActif(subscriptionTypeRequest.actif());
        type.setOrdre(subscriptionTypeRequest.ordre());
        return type;
    }

    public Page<SubscriptionTypeResponse> findResponses(SubscriptionTypeFilter filter) {
        return repository.findResponsesByFilter(filter, filter.toPageable());
    }

    public List<SubscriptionTypeResponse> findAllActifResponses() {
        return repository.findAllActifResponses();
    }

    public boolean existsByNom(String nom) {
        return repository.existsByNom(nom);
    }

    public TypeAbonnement setActive(TypeAbonnement type, boolean actif) {
        type.setActif(actif);
        return save(type);
    }
}
