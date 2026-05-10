package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.UtilisationCoupon;
import org.store.abonnement.domain.repository.UtilisationCouponJpaRepository;
import org.store.common.service.GlobalService;

@Service
public class UtilisationCouponDomainService extends GlobalService<UtilisationCoupon, UtilisationCouponJpaRepository> {
    public UtilisationCouponDomainService(UtilisationCouponJpaRepository repository) {
        super(repository);
    }
}
