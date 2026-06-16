package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.PlanAbonnementFilter;
import org.store.abonnement.application.dto.PlanAbonnementRequest;
import org.store.abonnement.application.dto.PlanAbonnementResponse;
import org.store.abonnement.application.dto.PublicPlanResponse;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.PlanAbonnementRepository;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;

import java.util.List;
import java.util.Optional;

@Service
public class PlanAbonnementDomainService extends GlobalService<PlanAbonnement, PlanAbonnementRepository> {
    public PlanAbonnementDomainService(PlanAbonnementRepository repository) {
        super(repository);
    }

    public Optional<PlanAbonnement> findFirstTrialActif() {
        return repository.findFirstByActifTrue();
    }

    public PlanAbonnement create(PlanAbonnementRequest planAbonnementRequest) {
        PlanAbonnement plan = new PlanAbonnement();
        applyRequest(plan, planAbonnementRequest);
        return save(plan);
    }

    public PlanAbonnement applyRequest(PlanAbonnement plan, PlanAbonnementRequest planAbonnementRequest) {
        plan.setNom(planAbonnementRequest.nom());
        plan.setDescription(planAbonnementRequest.description());
        plan.setPrix(planAbonnementRequest.prix());
        plan.setNombreMagasinsMax(planAbonnementRequest.nombreMagasinsMax());
        plan.setNombreEmployesMax(planAbonnementRequest.nombreEmployesMax());
        plan.setGestionStock(planAbonnementRequest.gestionStock());
        plan.setGestionVente(planAbonnementRequest.gestionVente());
        plan.setGestionAchat(planAbonnementRequest.gestionAchat());
        plan.setGestionComptabilite(planAbonnementRequest.gestionComptabilite());
        plan.setActif(planAbonnementRequest.actif());
        plan.setVisible(planAbonnementRequest.visible());
        plan.setOrdre(planAbonnementRequest.ordre());
        return plan;
    }

    public Page<PlanAbonnementResponse> findResponses(PlanAbonnementFilter filter) {
        return repository.findResponsesByFilter(filter.nom(), LikePatternHelper.toLikePattern(filter.nom()), filter.actif(), filter.visible(), filter.startDate(), filter.endDate(), filter.toPageable());
    }

    public List<PublicPlanResponse> findPublicResponses() {
        return repository.findPublicResponses();
    }

    /** Plans the OWNER can subscribe to (≥ 1 active non-trial type) — used by the subscribable catalog. */
    public List<PublicPlanResponse> findSubscribableResponses() {
        return repository.findSubscribableResponses();
    }

    public boolean existsByNom(String nom) {
        return repository.existsByNom(nom);
    }

    public PlanAbonnement setActive(PlanAbonnement plan, boolean actif) {
        plan.setActif(actif);
        return save(plan);
    }
}
