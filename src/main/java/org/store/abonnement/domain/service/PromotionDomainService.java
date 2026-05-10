package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.repository.PromotionJpaRepository;
import org.store.common.service.GlobalService;

@Service
public class PromotionDomainService extends GlobalService<Promotion, PromotionJpaRepository> {
    public PromotionDomainService(PromotionJpaRepository repository) {
        super(repository);
    }
}
