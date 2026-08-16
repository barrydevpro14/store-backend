package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.PlanAbonnementTarifRequest;
import org.store.abonnement.application.dto.PlanAbonnementTarifResponse;
import org.store.abonnement.application.service.IPlanAbonnementService;
import org.store.abonnement.application.service.IPlanAbonnementTarifService;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
import org.store.abonnement.domain.model.TarifAvecCoupon;
import org.store.abonnement.domain.service.PlanAbonnementTarifDomainService;
import org.store.common.exceptions.UniqueResourceException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Expose le CRUD des tarifs (plan + périodicité + prix) à la couche présentation.
 */
@Service
@Transactional(readOnly = true)
public class PlanAbonnementTarifServiceImpl implements IPlanAbonnementTarifService {

    private final PlanAbonnementTarifDomainService tarifDomainService;
    private final IPlanAbonnementService planAbonnementService;

    public PlanAbonnementTarifServiceImpl(PlanAbonnementTarifDomainService tarifDomainService,
                                          IPlanAbonnementService planAbonnementService) {
        this.tarifDomainService = tarifDomainService;
        this.planAbonnementService = planAbonnementService;
    }

    /** Délègue la résolution plan + périodicité → tarif au domain service. */
    @Override
    public Optional<PlanAbonnementTarif> findByPlanAndPeriodicite(PlanAbonnement plan, PeriodiciteAbonnement periodicite) {
        return tarifDomainService.findByPlanAndPeriodicite(plan, periodicite);
    }

    /** Retourne tous les tarifs d'un plan. */
    @Override
    public List<PlanAbonnementTarif> findByPlan(PlanAbonnement plan) {
        return tarifDomainService.findByPlan(plan);
    }

    /** Retourne tous les tarifs d'un plan sous forme de réponses. */
    @Override
    public List<PlanAbonnementTarifResponse> findResponsesByPlan(UUID planId) {
        PlanAbonnement plan = planAbonnementService.findById(planId);
        return tarifDomainService.findByPlan(plan).stream()
                .map(PlanAbonnementTarifResponse::new)
                .toList();
    }

    /** Lecture interne par id. */
    @Override
    public PlanAbonnementTarif findById(UUID id) {
        return tarifDomainService.findById(id);
    }

    /** Crée un tarif après contrôle d'unicité périodicité/plan. */
    @Override
    @Transactional
    public PlanAbonnementTarifResponse create(UUID planId, PlanAbonnementTarifRequest request) {
        PlanAbonnement plan = planAbonnementService.findById(planId);
        ensurePeriodiciteAvailable(plan, request.periodiciteAsEnum());
        return new PlanAbonnementTarifResponse(tarifDomainService.create(request, plan));
    }

    /** Met à jour un tarif ; revérifie l'unicité si la périodicité change. */
    @Override
    @Transactional
    public PlanAbonnementTarifResponse update(UUID planId, UUID tarifId, PlanAbonnementTarifRequest request) {
        PlanAbonnement plan = planAbonnementService.findById(planId);
        PlanAbonnementTarif tarif = tarifDomainService.findById(tarifId);

        if (tarif.getPeriodicite() != request.periodiciteAsEnum()) {
            ensurePeriodiciteAvailable(plan, request.periodiciteAsEnum());
        }

        return new PlanAbonnementTarifResponse(tarifDomainService.update(tarif, request));
    }

    /** Supprime un tarif après vérification d'appartenance au plan. */
    @Override
    @Transactional
    public void delete(UUID planId, UUID tarifId) {
        PlanAbonnementTarif tarif = tarifDomainService.findById(tarifId);
        tarifDomainService.delete(tarif);
    }

    /** Lève UniqueResourceException si la périodicité est déjà prise sur ce plan. */
    @Override
    public void ensurePeriodiciteAvailable(PlanAbonnement plan, PeriodiciteAbonnement periodicite) {
        if (tarifDomainService.existsByPlanAndPeriodicite(plan, periodicite)) {
            throw new UniqueResourceException("tarif.periodicite.alreadyExists", periodicite.name());
        }
    }

    @Override
    public List<TarifAvecCoupon> findActifWithCoupon(UUID planId) {
        return tarifDomainService.findActifWithCoupon(planId);
    }
}
