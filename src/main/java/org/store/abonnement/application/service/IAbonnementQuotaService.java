package org.store.abonnement.application.service;

import java.util.UUID;

public interface IAbonnementQuotaService {
    void ensureMagasinQuota(UUID entrepriseId);
    void ensureEmployeQuota(UUID entrepriseId, UUID magasinId);
}
