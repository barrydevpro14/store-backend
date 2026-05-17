package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.TypeAbonnement;
import org.store.abonnement.domain.repository.AbonnementRepository;
import org.store.common.service.GlobalService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.property.SubscriptionProperties;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class AbonnementDomainService extends GlobalService<Abonnement, AbonnementRepository> {

    private final SubscriptionProperties subscriptionProperties;

    public AbonnementDomainService(AbonnementRepository repository,
                                   SubscriptionProperties subscriptionProperties) {
        super(repository);
        this.subscriptionProperties = subscriptionProperties;
    }

    public Abonnement createTrial(Entreprise entreprise, PlanAbonnement plan) {
        Abonnement abonnement = new Abonnement();
        abonnement.setEntreprise(entreprise);
        abonnement.setPlan(plan);
        abonnement.setDateDebut(LocalDate.now());
        abonnement.setDateFin(LocalDate.now().plusDays(subscriptionProperties.trialDays()));
        abonnement.setActif(true);
        abonnement.setRenouvellementAuto(false);
        abonnement.setStatut(AbonnementStatut.ACTIF);
        return save(abonnement);
    }

    public Abonnement createPending(Entreprise entreprise, PlanAbonnement plan, TypeAbonnement type) {
        Abonnement abonnement = new Abonnement();
        abonnement.setEntreprise(entreprise);
        abonnement.setPlan(plan);
        abonnement.setTypeAbonnement(type);
        abonnement.setActif(false);
        abonnement.setRenouvellementAuto(false);
        abonnement.setStatut(AbonnementStatut.EN_ATTENTE);
        return save(abonnement);
    }

    public Abonnement setRenouvellementAuto(Abonnement abonnement, boolean enabled) {
        abonnement.setRenouvellementAuto(enabled);
        return save(abonnement);
    }

    public Abonnement activate(Abonnement abonnement, LocalDate dateDebut, LocalDate dateFin) {
        abonnement.setDateDebut(dateDebut);
        abonnement.setDateFin(dateFin);
        abonnement.setActif(true);
        abonnement.setStatut(AbonnementStatut.ACTIF);
        return save(abonnement);
    }

    public Optional<Abonnement> findCurrentActif(UUID entrepriseId) {
        return repository.findFirstByEntrepriseAndStatut(entrepriseId, AbonnementStatut.ACTIF);
    }

    public Optional<LocalDate> findLatestActifDateFin(UUID entrepriseId, UUID excludeAbonnementId) {
        return repository.findLatestActifDateFin(entrepriseId, excludeAbonnementId);
    }

    public Page<AbonnementResponse> findResponses(AbonnementFilter filter) {
        return repository.findResponsesByFilter(filter, filter.toPageable());
    }
}
