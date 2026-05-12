package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.AbonnementRepository;
import org.store.common.service.GlobalService;
import org.store.entreprise.domain.model.Entreprise;

import java.time.LocalDate;

@Service
public class AbonnementDomainService extends GlobalService<Abonnement, AbonnementRepository> {

    private static final int TRIAL_DAYS = 30;

    public AbonnementDomainService(AbonnementRepository repository) {
        super(repository);
    }

    public Abonnement createTrial(Entreprise entreprise, PlanAbonnement plan) {
        Abonnement abonnement = new Abonnement();
        abonnement.setEntreprise(entreprise);
        abonnement.setPlan(plan);
        abonnement.setDateDebut(LocalDate.now());
        abonnement.setDateFin(LocalDate.now().plusDays(TRIAL_DAYS));
        abonnement.setActif(true);
        abonnement.setRenouvellementAuto(false);
        abonnement.setStatut(AbonnementStatut.ACTIF);
        return save(abonnement);
    }
}
