package org.store.abonnement.application.service.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.PlanAbonnementFilter;
import org.store.abonnement.application.dto.PlanAbonnementRequest;
import org.store.abonnement.application.dto.PlanAbonnementResponse;
import org.store.abonnement.application.service.IPlanAbonnementService;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.service.ValidatorService;
import org.store.config.RedisCacheConfig;

import java.util.UUID;

/**
 * Gère le catalogue des plans d'abonnement (référentiel global, ADMIN-only).
 */
@Service
@Transactional(readOnly = true)
public class PlanAbonnementServiceImpl implements IPlanAbonnementService {

    private final PlanAbonnementDomainService planAbonnementDomainService;
    private final ValidatorService validatorService;

    public PlanAbonnementServiceImpl(PlanAbonnementDomainService planAbonnementDomainService,
                                     ValidatorService validatorService) {
        this.planAbonnementDomainService = planAbonnementDomainService;
        this.validatorService = validatorService;
    }

    /** Retourne le premier plan d'essai actif ou throw `plan.trial.notFound`. */
    @Override
    public PlanAbonnement findFirstTrialActif() {
        return planAbonnementDomainService.findFirstTrialActif()
                .orElseThrow(() -> new EntityException("plan.trial.notFound"));
    }

    /** Crée un plan après contrôle d'unicité du nom. Invalide le cache du catalogue public. */
    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.PUBLIC_CATALOG, allEntries = true)
    public PlanAbonnementResponse create(PlanAbonnementRequest planAbonnementRequest) {
        ensureNomAvailable(planAbonnementRequest.nom());
        return new PlanAbonnementResponse(planAbonnementDomainService.create(planAbonnementRequest));
    }

    /** Délègue au domain service ; lève `EntityException` si introuvable. */
    @Override
    public PlanAbonnement findById(UUID id) {
        return planAbonnementDomainService.findById(id);
    }

    /** Retourne le plan en `Response`. */
    @Override
    public PlanAbonnementResponse findResponseById(UUID id) {
        return new PlanAbonnementResponse(planAbonnementDomainService.findById(id));
    }

    /** Liste paginée triée par ordre puis nom. */
    @Override
    public Page<PlanAbonnementResponse> findAll(PlanAbonnementFilter filter) {
        validatorService.validate(filter);
        return planAbonnementDomainService.findResponses(filter);
    }

    /** Met à jour un plan ; revérifie l'unicité du nom si modifié. Invalide le cache du catalogue public. */
    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.PUBLIC_CATALOG, allEntries = true)
    public PlanAbonnementResponse update(UUID id, PlanAbonnementRequest planAbonnementRequest) {
        PlanAbonnement plan = planAbonnementDomainService.findById(id);

        if (!plan.getNom().equals(planAbonnementRequest.nom())) {
            ensureNomAvailable(planAbonnementRequest.nom());
        }

        planAbonnementDomainService.applyRequest(plan, planAbonnementRequest);
        return new PlanAbonnementResponse(planAbonnementDomainService.save(plan));
    }

    /** Force `actif=true` sur le plan ciblé. Invalide le cache du catalogue public. */
    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.PUBLIC_CATALOG, allEntries = true)
    public PlanAbonnementResponse activate(UUID id) {
        PlanAbonnement plan = planAbonnementDomainService.findById(id);
        return new PlanAbonnementResponse(planAbonnementDomainService.setActive(plan, true));
    }

    /** Force `actif=false` sur le plan ciblé. Invalide le cache du catalogue public. */
    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.PUBLIC_CATALOG, allEntries = true)
    public PlanAbonnementResponse deactivate(UUID id) {
        PlanAbonnement plan = planAbonnementDomainService.findById(id);
        return new PlanAbonnementResponse(planAbonnementDomainService.setActive(plan, false));
    }

    /** Supprime un plan ; peut échouer si référencé par un abonnement existant. Invalide le cache du catalogue public. */
    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.PUBLIC_CATALOG, allEntries = true)
    public void delete(UUID id) {
        PlanAbonnement plan = planAbonnementDomainService.findById(id);
        planAbonnementDomainService.delete(plan);
    }

    /** Throw `UniqueResourceException` si un plan porte déjà ce nom. */
    @Override
    public void ensureNomAvailable(String nom) {
        if (planAbonnementDomainService.existsByNom(nom)) {
            throw new UniqueResourceException("plan.nom.alreadyExists", nom);
        }
    }
}
