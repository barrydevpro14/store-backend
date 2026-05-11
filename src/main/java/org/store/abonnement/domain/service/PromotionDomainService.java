package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.repository.PromotionRepository;
import org.store.common.service.GlobalService;

@Service
public class PromotionDomainService extends GlobalService<Promotion, PromotionRepository> {
    public PromotionDomainService(PromotionRepository repository) {
        super(repository);
    }
}
