package org.store.abonnement.application.service.impl;

import org.store.abonnement.application.service.*;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.entreprise.domain.model.Entreprise;

@Service
public class AbonnementServiceImpl implements IAbonnementService {

    private final AbonnementDomainService abonnementDomainService;

    public AbonnementServiceImpl(AbonnementDomainService abonnementDomainService) {
        this.abonnementDomainService = abonnementDomainService;
    }

    @Override
    public Abonnement createTrial(Entreprise entreprise, PlanAbonnement plan) {
        return abonnementDomainService.createTrial(entreprise, plan);
    }
}
