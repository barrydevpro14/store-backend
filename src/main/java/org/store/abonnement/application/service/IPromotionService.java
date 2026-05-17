package org.store.abonnement.application.service;

import org.springframework.data.domain.Page;
import org.store.abonnement.application.dto.PromotionFilter;
import org.store.abonnement.application.dto.PromotionRequest;
import org.store.abonnement.application.dto.PromotionResponse;
import org.store.abonnement.domain.model.Promotion;

import java.util.UUID;

public interface IPromotionService {

    /**
     * Création d'une promotion (réduction automatique liée à un plan optionnel). Période et cohérence réduction validées.
     */
    PromotionResponse create(PromotionRequest promotionRequest);

    /**
     * Lecture interne par id.
     */
    Promotion findById(UUID id);

    /**
     * Lecture par id en `Response`.
     */
    PromotionResponse findResponseById(UUID id);

    /**
     * Listing paginé filtré.
     */
    Page<PromotionResponse> findAll(PromotionFilter filter);

    /**
     * Mise à jour. Période et cohérence réduction revalidées.
     */
    PromotionResponse update(UUID id, PromotionRequest promotionRequest);

    /**
     * Activation.
     */
    PromotionResponse activate(UUID id);

    /**
     * Désactivation.
     */
    PromotionResponse deactivate(UUID id);

    /**
     * Suppression.
     */
    void delete(UUID id);
}
