package org.store.abonnement.application.service;

import org.springframework.data.domain.Page;
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.domain.model.TypePlanAbonnement;

import java.util.UUID;

public interface ISubscriptionTypeService {

    /** Creates a type scoped to the given plan; enforces uniqueness on (planId, nom). */
    SubscriptionTypeResponse create(UUID planId, SubscriptionTypeRequest subscriptionTypeRequest);

    /** Internal lookup by id (used by Abonnement / Paiement). */
    TypePlanAbonnement findById(UUID id);

    /** Reads a type by id as Response; throws on missing or plan mismatch. */
    SubscriptionTypeResponse findResponseById(UUID planId, UUID id);

    /** Paginated listing filtered to the given plan. */
    Page<SubscriptionTypeResponse> findAll(UUID planId, SubscriptionTypeFilter filter);

    /** Updates the type; re-checks (planId, nom) uniqueness on rename. */
    SubscriptionTypeResponse update(UUID planId, UUID id, SubscriptionTypeRequest subscriptionTypeRequest);

    /** Marks the type active. */
    SubscriptionTypeResponse activate(UUID planId, UUID id);

    /** Marks the type inactive. */
    SubscriptionTypeResponse deactivate(UUID planId, UUID id);

    /** Deletes the type. */
    void delete(UUID planId, UUID id);

    /** Throws {@code UniqueResourceException} if another type already carries this name in the same plan. */
    void ensureNomAvailable(UUID planId, String nom);

    /** Update variant: tolerates the same name on the entity itself, rejects collisions on any other row. */
    void ensureNomAvailableForUpdate(UUID planId, String nom, UUID id);

    /** Throws {@code BadArgumentException} if the type does not belong to the expected plan. */
    void ensureBelongsToPlan(TypePlanAbonnement type, UUID planId);
}
