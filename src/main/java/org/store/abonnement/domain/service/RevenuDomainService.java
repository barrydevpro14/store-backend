package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.RevenuPeriodFilter;
import org.store.abonnement.domain.model.Revenu;
import org.store.abonnement.domain.repository.RevenuRepository;
import org.store.common.service.GlobalService;

import java.math.BigDecimal;

@Service
public class RevenuDomainService extends GlobalService<Revenu, RevenuRepository> {
    public RevenuDomainService(RevenuRepository repository) {
        super(repository);
    }

    public BigDecimal sumByPeriod(RevenuPeriodFilter filter) {
        return repository.sumByPeriod(filter.startDate(), filter.endDate(), filter.countryId(), filter.entrepriseId());
    }
}
