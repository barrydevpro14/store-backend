package org.store.abonnement.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.PromotionFilter;
import org.store.abonnement.application.dto.PromotionRequest;
import org.store.abonnement.application.dto.PromotionResponse;
import org.store.abonnement.application.service.IPlanAbonnementService;
import org.store.abonnement.application.service.IPromotionService;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.service.PromotionDomainService;
import org.store.common.service.ValidatorService;
import org.store.common.tools.SubscriptionRules;

import java.util.UUID;

/**
 * Gère le catalogue des promotions (réductions automatiques sur un plan optionnel), ADMIN-only.
 */
@Service
@Transactional(readOnly = true)
public class PromotionServiceImpl implements IPromotionService {

    private final PromotionDomainService promotionDomainService;
    private final IPlanAbonnementService planAbonnementService;
    private final ValidatorService validatorService;

    public PromotionServiceImpl(PromotionDomainService promotionDomainService,
                                IPlanAbonnementService planAbonnementService,
                                ValidatorService validatorService) {
        this.promotionDomainService = promotionDomainService;
        this.planAbonnementService = planAbonnementService;
        this.validatorService = validatorService;
    }

    /** Crée une promotion après validation période + cohérence réduction et résolution du plan optionnel. */
    @Override
    @Transactional
    public PromotionResponse create(PromotionRequest promotionRequest) {
        SubscriptionRules.ensurePeriodValid(
                promotionRequest.dateDebut(), promotionRequest.dateFin(), "promotion.invalidPeriod");
        SubscriptionRules.ensureReductionConsistent(
                promotionRequest.reductionTypeAsEnum(),
                promotionRequest.valeurReduction(),
                "promotion.reduction.invalid");

        PlanAbonnement plan = planAbonnementService.findByIdOrNull(promotionRequest.planId());
        return new PromotionResponse(promotionDomainService.create(promotionRequest, plan));
    }

    /** Délègue au domain service. */
    @Override
    public Promotion findById(UUID id) {
        return promotionDomainService.findById(id);
    }

    /** Retourne la promotion en `Response`. */
    @Override
    public PromotionResponse findResponseById(UUID id) {
        return new PromotionResponse(promotionDomainService.findById(id));
    }

    /** Liste paginée filtrée. */
    @Override
    public Page<PromotionResponse> findAll(PromotionFilter filter) {
        validatorService.validate(filter);
        return promotionDomainService.findResponses(filter);
    }

    /** Met à jour ; revérifie période et cohérence. */
    @Override
    @Transactional
    public PromotionResponse update(UUID id, PromotionRequest promotionRequest) {
        Promotion promotion = promotionDomainService.findById(id);

        SubscriptionRules.ensurePeriodValid(
                promotionRequest.dateDebut(), promotionRequest.dateFin(), "promotion.invalidPeriod");
        SubscriptionRules.ensureReductionConsistent(
                promotionRequest.reductionTypeAsEnum(),
                promotionRequest.valeurReduction(),
                "promotion.reduction.invalid");

        PlanAbonnement plan = planAbonnementService.findByIdOrNull(promotionRequest.planId());
        promotionDomainService.applyRequest(promotion, promotionRequest, plan);
        return new PromotionResponse(promotionDomainService.save(promotion));
    }

    /** Force `actif=true`. */
    @Override
    @Transactional
    public PromotionResponse activate(UUID id) {
        Promotion promotion = promotionDomainService.findById(id);
        return new PromotionResponse(promotionDomainService.setActive(promotion, true));
    }

    /** Force `actif=false`. */
    @Override
    @Transactional
    public PromotionResponse deactivate(UUID id) {
        Promotion promotion = promotionDomainService.findById(id);
        return new PromotionResponse(promotionDomainService.setActive(promotion, false));
    }

    /** Supprime la promotion. */
    @Override
    @Transactional
    public void delete(UUID id) {
        Promotion promotion = promotionDomainService.findById(id);
        promotionDomainService.delete(promotion);
    }
}
