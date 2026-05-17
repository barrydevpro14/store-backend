package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.PlanAbonnementFilter;
import org.store.abonnement.application.dto.PlanAbonnementRequest;
import org.store.abonnement.application.dto.PlanAbonnementResponse;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.PlanAbonnementRepository;
import org.store.common.service.GlobalService;

import java.util.List;
import java.util.Optional;

@Service
public class PlanAbonnementDomainService extends GlobalService<PlanAbonnement, PlanAbonnementRepository> {
    public PlanAbonnementDomainService(PlanAbonnementRepository repository) {
        super(repository);
    }

    public Optional<PlanAbonnement> findFirstTrialActif() {
        return repository.findFirstByTrialTrueAndActifTrue();
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
        plan.setTrial(planAbonnementRequest.trial());
        plan.setOrdre(planAbonnementRequest.ordre());
        return plan;
    }

    public Page<PlanAbonnementResponse> findResponses(PlanAbonnementFilter filter) {
        return repository.findResponsesByFilter(filter, filter.toPageable());
    }

    public List<PlanAbonnement> findAllVisibleAndActif() {
        return repository.findAllVisibleAndActif();
    }

    public boolean existsByNom(String nom) {
        return repository.existsByNom(nom);
    }

    public PlanAbonnement setActive(PlanAbonnement plan, boolean actif) {
        plan.setActif(actif);
        return save(plan);
    }
}
