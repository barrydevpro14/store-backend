package org.store.abonnement.application.service;

import org.springframework.data.domain.Page;
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.domain.model.TypeAbonnement;

import java.util.UUID;

public interface ISubscriptionTypeService {

    /**
     * Création d'un type d'abonnement (durée + réduction intégrée). Unicité du nom contrôlée.
     */
    SubscriptionTypeResponse create(SubscriptionTypeRequest subscriptionTypeRequest);

    /**
     * Lecture interne par id (utilisée par Abonnement / Paiement).
     */
    TypeAbonnement findById(UUID id);

    /**
     * Lecture par id en `Response`. Throw `EntityException("subscriptionType.notFound")` si introuvable.
     */
    SubscriptionTypeResponse findResponseById(UUID id);

    /**
     * Listing paginé filtré.
     */
    Page<SubscriptionTypeResponse> findAll(SubscriptionTypeFilter filter);

    /**
     * Mise à jour. Unicité du nom revérifiée si changement.
     */
    SubscriptionTypeResponse update(UUID id, SubscriptionTypeRequest subscriptionTypeRequest);

    /**
     * Activation du type.
     */
    SubscriptionTypeResponse activate(UUID id);

    /**
     * Désactivation du type.
     */
    SubscriptionTypeResponse deactivate(UUID id);

    /**
     * Suppression.
     */
    void delete(UUID id);

    /**
     * Throw `UniqueResourceException("subscriptionType.nom.alreadyExists")` si un type porte déjà ce nom.
     */
    void ensureNomAvailable(String nom);
}
