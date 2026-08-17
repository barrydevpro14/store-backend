package org.store.abonnement.application.service;

import org.store.abonnement.domain.model.PlanAbonnement;

import java.util.UUID;

public interface IAbonnementQuotaService {
    void ensureMagasinQuota(UUID entrepriseId);
    void ensureEmployeQuota(UUID entrepriseId, UUID magasinId);
    void ensureMagasinQuotaForPlan(UUID entrepriseId, PlanAbonnement plan);
}
