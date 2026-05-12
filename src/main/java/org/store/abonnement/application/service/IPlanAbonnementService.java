package org.store.abonnement.application.service;

import org.store.abonnement.domain.model.PlanAbonnement;

public interface IPlanAbonnementService {

    PlanAbonnement findFirstTrialActif();
}
