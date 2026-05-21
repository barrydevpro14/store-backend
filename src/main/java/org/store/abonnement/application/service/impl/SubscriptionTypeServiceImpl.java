package org.store.abonnement.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.application.service.IPlanAbonnementService;
import org.store.abonnement.application.service.ISubscriptionTypeService;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.TypePlanAbonnement;
import org.store.abonnement.domain.service.TypePlanAbonnementDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.service.ValidatorService;
import org.store.common.tools.SubscriptionRules;

import java.util.UUID;

/**
 * Manages the catalog of subscription durations attached to a plan (ADMIN-only).
 * Every operation is plan-scoped via the {@code planId} carried by the nested REST path.
 */
@Service
@Transactional(readOnly = true)
public class SubscriptionTypeServiceImpl implements ISubscriptionTypeService {

    private final TypePlanAbonnementDomainService typePlanAbonnementDomainService;
    private final IPlanAbonnementService planAbonnementService;
    private final ValidatorService validatorService;

    public SubscriptionTypeServiceImpl(TypePlanAbonnementDomainService typePlanAbonnementDomainService,
                                       IPlanAbonnementService planAbonnementService,
                                       ValidatorService validatorService) {
        this.typePlanAbonnementDomainService = typePlanAbonnementDomainService;
        this.planAbonnementService = planAbonnementService;
        this.validatorService = validatorService;
    }

    /** Creates a type attached to the given plan after enforcing (planId, nom) uniqueness and reduction consistency. */
    @Override
    @Transactional
    public SubscriptionTypeResponse create(UUID planId, SubscriptionTypeRequest subscriptionTypeRequest) {
        PlanAbonnement plan = planAbonnementService.findById(planId);

        ensureNomAvailable(planId, subscriptionTypeRequest.nom());
        SubscriptionRules.ensureReductionConsistent(
                subscriptionTypeRequest.reductionTypeAsEnum(),
                subscriptionTypeRequest.valeurReduction(),
                "subscriptionType.reduction.invalid");

        return new SubscriptionTypeResponse(
                typePlanAbonnementDomainService.create(plan, subscriptionTypeRequest));
    }

    /** Delegates to the domain service; throws {@code EntityException} when missing. */
    @Override
    public TypePlanAbonnement findById(UUID id) {
        return typePlanAbonnementDomainService.findById(id);
    }

    /** Loads a type, validates it belongs to the given plan, and projects it as a Response. */
    @Override
    public SubscriptionTypeResponse findResponseById(UUID planId, UUID id) {
        TypePlanAbonnement type = typePlanAbonnementDomainService.findById(id);
        ensureBelongsToPlan(type, planId);
        return new SubscriptionTypeResponse(type);
    }

    /** Paginated listing scoped to the plan, sorted by ordre then nom. */
    @Override
    public Page<SubscriptionTypeResponse> findAll(UUID planId, SubscriptionTypeFilter filter) {
        validatorService.validate(filter);
        planAbonnementService.findById(planId);
        return typePlanAbonnementDomainService.findResponses(planId, filter);
    }

    /** Updates an existing type; re-checks (planId, nom) uniqueness on rename and reduction consistency. */
    @Override
    @Transactional
    public SubscriptionTypeResponse update(UUID planId, UUID id, SubscriptionTypeRequest subscriptionTypeRequest) {
        TypePlanAbonnement type = typePlanAbonnementDomainService.findById(id);
        ensureBelongsToPlan(type, planId);

        if (!type.getNom().equals(subscriptionTypeRequest.nom())) {
            ensureNomAvailableForUpdate(planId, subscriptionTypeRequest.nom(), id);
        }

        SubscriptionRules.ensureReductionConsistent(
                subscriptionTypeRequest.reductionTypeAsEnum(),
                subscriptionTypeRequest.valeurReduction(),
                "subscriptionType.reduction.invalid");

        typePlanAbonnementDomainService.applyRequest(type, subscriptionTypeRequest);
        return new SubscriptionTypeResponse(typePlanAbonnementDomainService.save(type));
    }

    /** Forces {@code actif=true}. */
    @Override
    @Transactional
    public SubscriptionTypeResponse activate(UUID planId, UUID id) {
        TypePlanAbonnement type = typePlanAbonnementDomainService.findById(id);
        ensureBelongsToPlan(type, planId);
        return new SubscriptionTypeResponse(typePlanAbonnementDomainService.setActive(type, true));
    }

    /** Forces {@code actif=false}. */
    @Override
    @Transactional
    public SubscriptionTypeResponse deactivate(UUID planId, UUID id) {
        TypePlanAbonnement type = typePlanAbonnementDomainService.findById(id);
        ensureBelongsToPlan(type, planId);
        return new SubscriptionTypeResponse(typePlanAbonnementDomainService.setActive(type, false));
    }

    /** Deletes the type. */
    @Override
    @Transactional
    public void delete(UUID planId, UUID id) {
        TypePlanAbonnement type = typePlanAbonnementDomainService.findById(id);
        ensureBelongsToPlan(type, planId);
        typePlanAbonnementDomainService.delete(type);
    }

    /** Throws {@code UniqueResourceException} if another type already carries this name in the same plan. */
    @Override
    public void ensureNomAvailable(UUID planId, String nom) {
        if (typePlanAbonnementDomainService.existsByPlanIdAndNom(planId, nom)) {
            throw new UniqueResourceException("subscriptionType.nom.alreadyExists", nom);
        }
    }

    /** Update variant: tolerates the same name on the entity itself, rejects collisions on any other row. */
    @Override
    public void ensureNomAvailableForUpdate(UUID planId, String nom, UUID id) {
        if (typePlanAbonnementDomainService.existsByPlanIdAndNomAndIdNot(planId, nom, id)) {
            throw new UniqueResourceException("subscriptionType.nom.alreadyExists", nom);
        }
    }

    /** Throws {@code BadArgumentException} if the type does not belong to the expected plan. */
    @Override
    public void ensureBelongsToPlan(TypePlanAbonnement type, UUID planId) {
        if (!type.getPlan().getId().equals(planId)) {
            throw new BadArgumentException("subscriptionType.planMismatch");
        }
    }
}
