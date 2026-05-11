package org.store.abonnement.application.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.AbonnementRepository;
import org.store.entreprise.domain.model.Entreprise;

import java.time.LocalDate;

@Service
public class AbonnementServiceImpl implements IAbonnementService {

    private static final int TRIAL_DAYS = 30;

    private final AbonnementRepository abonnementRepository;

    public AbonnementServiceImpl(AbonnementRepository abonnementRepository) {
        this.abonnementRepository = abonnementRepository;
    }

    @Override
    public Abonnement createTrial(Entreprise entreprise, PlanAbonnement plan) {
        Abonnement abonnement = new Abonnement();
        abonnement.setEntreprise(entreprise);
        abonnement.setPlan(plan);
        abonnement.setDateDebut(LocalDate.now());
        abonnement.setDateFin(LocalDate.now().plusDays(TRIAL_DAYS));
        abonnement.setActif(true);
        abonnement.setRenouvellementAuto(false);
        abonnement.setStatut(AbonnementStatut.ACTIF);
        return abonnementRepository.save(abonnement);
    }
}
