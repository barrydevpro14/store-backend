package org.store.abonnement.application.service.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.application.service.ISubscriptionTypeService;
import org.store.common.tools.SubscriptionRules;
import org.store.abonnement.domain.model.TypeAbonnement;
import org.store.abonnement.domain.service.TypeAbonnementDomainService;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.service.ValidatorService;
import org.store.config.RedisCacheConfig;

import java.util.UUID;

/**
 * Gère le catalogue des types d'abonnement (durées + réductions intégrées), ADMIN-only.
 */
@Service
@Transactional(readOnly = true)
public class SubscriptionTypeServiceImpl implements ISubscriptionTypeService {

    private final TypeAbonnementDomainService typeAbonnementDomainService;
    private final ValidatorService validatorService;

    public SubscriptionTypeServiceImpl(TypeAbonnementDomainService typeAbonnementDomainService,
                                       ValidatorService validatorService) {
        this.typeAbonnementDomainService = typeAbonnementDomainService;
        this.validatorService = validatorService;
    }

    /** Crée un type après vérification d'unicité du nom et cohérence type/valeur de réduction. Invalide le cache du catalogue public. */
    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.PUBLIC_CATALOG, allEntries = true)
    public SubscriptionTypeResponse create(SubscriptionTypeRequest subscriptionTypeRequest) {
        ensureNomAvailable(subscriptionTypeRequest.nom());
        SubscriptionRules.ensureReductionConsistent(
                subscriptionTypeRequest.reductionTypeAsEnum(),
                subscriptionTypeRequest.valeurReduction(),
                "subscriptionType.reduction.invalid");
        return new SubscriptionTypeResponse(typeAbonnementDomainService.create(subscriptionTypeRequest));
    }

    /** Délégué au domain service ; lève `EntityException` si introuvable. */
    @Override
    public TypeAbonnement findById(UUID id) {
        return typeAbonnementDomainService.findById(id);
    }

    /** Retourne le type en `Response`. */
    @Override
    public SubscriptionTypeResponse findResponseById(UUID id) {
        return new SubscriptionTypeResponse(typeAbonnementDomainService.findById(id));
    }

    /** Liste paginée filtrée triée par ordre puis nom. */
    @Override
    public Page<SubscriptionTypeResponse> findAll(SubscriptionTypeFilter filter) {
        validatorService.validate(filter);
        return typeAbonnementDomainService.findResponses(filter);
    }

    /** Met à jour ; revérifie l'unicité du nom (si changé) et la cohérence de la réduction. Invalide le cache du catalogue public. */
    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.PUBLIC_CATALOG, allEntries = true)
    public SubscriptionTypeResponse update(UUID id, SubscriptionTypeRequest subscriptionTypeRequest) {
        TypeAbonnement type = typeAbonnementDomainService.findById(id);

        if (!type.getNom().equals(subscriptionTypeRequest.nom())) {
            ensureNomAvailable(subscriptionTypeRequest.nom());
        }

        SubscriptionRules.ensureReductionConsistent(
                subscriptionTypeRequest.reductionTypeAsEnum(),
                subscriptionTypeRequest.valeurReduction(),
                "subscriptionType.reduction.invalid");

        typeAbonnementDomainService.applyRequest(type, subscriptionTypeRequest);
        return new SubscriptionTypeResponse(typeAbonnementDomainService.save(type));
    }

    /** Force `actif=true`. Invalide le cache du catalogue public. */
    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.PUBLIC_CATALOG, allEntries = true)
    public SubscriptionTypeResponse activate(UUID id) {
        TypeAbonnement type = typeAbonnementDomainService.findById(id);
        return new SubscriptionTypeResponse(typeAbonnementDomainService.setActive(type, true));
    }

    /** Force `actif=false`. Invalide le cache du catalogue public. */
    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.PUBLIC_CATALOG, allEntries = true)
    public SubscriptionTypeResponse deactivate(UUID id) {
        TypeAbonnement type = typeAbonnementDomainService.findById(id);
        return new SubscriptionTypeResponse(typeAbonnementDomainService.setActive(type, false));
    }

    /** Supprime le type. Invalide le cache du catalogue public. */
    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.PUBLIC_CATALOG, allEntries = true)
    public void delete(UUID id) {
        TypeAbonnement type = typeAbonnementDomainService.findById(id);
        typeAbonnementDomainService.delete(type);
    }

    /** Lève `UniqueResourceException` si un type porte déjà ce nom. */
    @Override
    public void ensureNomAvailable(String nom) {
        if (typeAbonnementDomainService.existsByNom(nom)) {
            throw new UniqueResourceException("subscriptionType.nom.alreadyExists", nom);
        }
    }
}
