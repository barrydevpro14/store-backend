package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.TypePlanAbonnement;
import org.store.abonnement.domain.repository.TypePlanAbonnementRepository;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain service for {@code TypePlanAbonnement} (the duration + integrated discount of a plan). Every
 * operation is plan-scoped, so a same name ("Mensuel", "Annuel") can coexist on several plans without
 * colliding.
 */
@Service
public class TypePlanAbonnementDomainService extends GlobalService<TypePlanAbonnement, TypePlanAbonnementRepository> {

    public TypePlanAbonnementDomainService(TypePlanAbonnementRepository repository) {
        super(repository);
    }

    /** Creates a type attached to the given plan and persists it. */
    public TypePlanAbonnement create(PlanAbonnement plan, SubscriptionTypeRequest subscriptionTypeRequest) {
        TypePlanAbonnement type = new TypePlanAbonnement();
        type.setPlan(plan);
        applyRequest(type, subscriptionTypeRequest);
        return save(type);
    }

    /** Copies the request fields onto the entity (without touching the plan). */
    public TypePlanAbonnement applyRequest(TypePlanAbonnement type, SubscriptionTypeRequest subscriptionTypeRequest) {
        type.setNom(subscriptionTypeRequest.nom());
        type.setDureeMois(subscriptionTypeRequest.dureeMois());
        type.setReductionType(subscriptionTypeRequest.reductionTypeAsEnum());
        type.setValeurReduction(subscriptionTypeRequest.valeurReduction());
        type.setRecommande(subscriptionTypeRequest.recommande());
        type.setActif(subscriptionTypeRequest.actif());
        type.setOrdre(subscriptionTypeRequest.ordre());
        return type;
    }

    /** Plan-scoped paginated listing, filtered by nom/actif/recommande. */
    public Page<SubscriptionTypeResponse> findResponses(UUID planId, SubscriptionTypeFilter filter) {
        return repository.findResponsesByFilter(planId, filter.nom(), LikePatternHelper.toLikePattern(filter.nom()), filter.actif(), filter.recommande(), filter.startDate(), filter.endDate(), filter.toPageable());
    }

    /** Active durations of a plan (sorted) — used by the public catalog. */
    public List<SubscriptionTypeResponse> findActifResponsesByPlanId(UUID planId) {
        return repository.findActifResponsesByPlanId(planId);
    }

    /** Active non-trial durations of a plan — used by the OWNER subscribable catalog. */
    public List<SubscriptionTypeResponse> findActifNonTrialResponsesByPlanId(UUID planId) {
        return repository.findActifNonTrialResponsesByPlanId(planId);
    }

    /** True if another type already carries this name in the same plan. */
    public boolean existsByPlanIdAndNom(UUID planId, String nom) {
        return repository.existsByPlanIdAndNom(planId, nom);
    }

    /** True if another type (different id) already carries this name in the same plan. */
    public boolean existsByPlanIdAndNomAndIdNot(UUID planId, String nom, UUID id) {
        return repository.existsByPlanIdAndNomAndIdNot(planId, nom, id);
    }

    /** Forces {@code actif=true/false} and persists. */
    public TypePlanAbonnement setActive(TypePlanAbonnement type, boolean actif) {
        type.setActif(actif);
        return save(type);
    }

    /** Returns the active trial type — the one carrying {@code trial=true} (used by OWNER signup flow). */
    public Optional<TypePlanAbonnement> findFirstActifTrial() {
        return repository.findByTrial();
    }
}
