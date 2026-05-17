package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.PromotionFilter;
import org.store.abonnement.application.dto.PromotionRequest;
import org.store.abonnement.application.dto.PromotionResponse;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.repository.PromotionRepository;
import org.store.common.service.GlobalService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PromotionDomainService extends GlobalService<Promotion, PromotionRepository> {
    public PromotionDomainService(PromotionRepository repository) {
        super(repository);
    }

    public Promotion create(PromotionRequest promotionRequest, PlanAbonnement plan) {
        Promotion promotion = new Promotion();
        applyRequest(promotion, promotionRequest, plan);
        return save(promotion);
    }

    public Promotion applyRequest(Promotion promotion, PromotionRequest promotionRequest, PlanAbonnement plan) {
        promotion.setNom(promotionRequest.nom());
        promotion.setDescription(promotionRequest.description());
        promotion.setReductionType(promotionRequest.reductionTypeAsEnum());
        promotion.setValeurReduction(promotionRequest.valeurReduction());
        promotion.setDateDebut(promotionRequest.dateDebut());
        promotion.setDateFin(promotionRequest.dateFin());
        promotion.setActif(promotionRequest.actif());
        promotion.setPlan(plan);
        return promotion;
    }

    public Page<PromotionResponse> findResponses(PromotionFilter filter) {
        return repository.findResponsesByFilter(filter, filter.toPageable());
    }

    public List<PromotionResponse> findAllActifResponses(LocalDate today) {
        return repository.findAllActifResponses(today);
    }

    public Optional<Promotion> findFirstActivePromotionForPlan(UUID planId, LocalDate today) {
        return repository.findFirstActivePromotionForPlan(planId, today);
    }

    public Promotion setActive(Promotion promotion, boolean actif) {
        promotion.setActif(actif);
        return save(promotion);
    }
}
