package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.Revenu;
import org.store.abonnement.domain.repository.RevenuRepository;
import org.store.common.service.GlobalService;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class RevenuDomainService extends GlobalService<Revenu, RevenuRepository> {
    public RevenuDomainService(RevenuRepository repository) {
        super(repository);
    }

    public BigDecimal sumByPeriod(String startDate, String endDate, UUID countryId, UUID entrepriseId) {
        return repository.sumByPeriod(startDate, endDate, countryId, entrepriseId);
    }
}
