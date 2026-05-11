package org.store.abonnement.application.service;

import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.entreprise.domain.model.Entreprise;

public interface IAbonnementService {

    Abonnement createTrial(Entreprise entreprise, PlanAbonnement plan);
}
