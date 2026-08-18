package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.PlanAbonnementTarifRequest;
import org.store.abonnement.application.dto.PlanAbonnementTarifResponse;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
import org.store.abonnement.domain.model.TarifAvecCoupon;
import org.store.abonnement.domain.repository.PlanAbonnementTarifRepository;
import org.store.common.service.GlobalService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gère les tarifs (plan + périodicité + prix) du catalogue d'abonnements.
 */
@Service
public class PlanAbonnementTarifDomainService extends GlobalService<PlanAbonnementTarif, PlanAbonnementTarifRepository> {

    public PlanAbonnementTarifDomainService(PlanAbonnementTarifRepository repository) {
        super(repository);
    }

    public Optional<PlanAbonnementTarif> findByPlanAndPeriodicite(PlanAbonnement plan, PeriodiciteAbonnement periodicite) {
        return repository.findByPlanAndPeriodicite(plan, periodicite);
    }

    public boolean existsByPlanAndPeriodicite(PlanAbonnement plan, PeriodiciteAbonnement periodicite) {
        return repository.existsByPlanAndPeriodicite(plan, periodicite);
    }

    public List<PlanAbonnementTarifResponse> findByPlan(UUID planId) {
        return repository.findByPlan(planId);
    }

    public List<TarifAvecCoupon> findActifWithCoupon(UUID planId) {
        return repository.findActifWithCoupon(planId, LocalDate.now());
    }

    public PlanAbonnementTarif create(PlanAbonnementTarifRequest request, PlanAbonnement plan) {
        PlanAbonnementTarif tarif = new PlanAbonnementTarif();
        tarif.setPlan(plan);
        return save(applyRequest(tarif, request));
    }

    public PlanAbonnementTarif update(PlanAbonnementTarif tarif, PlanAbonnementTarifRequest request) {
        return save(applyRequest(tarif, request));
    }

    private PlanAbonnementTarif applyRequest(PlanAbonnementTarif tarif, PlanAbonnementTarifRequest request) {
        tarif.setPeriodicite(request.periodiciteAsEnum());
        tarif.setPrix(request.prix());
        tarif.setActif(request.actif());
        tarif.setRecommande(request.recommande());
        tarif.setOrdre(request.ordre());
        return tarif;
    }
}
