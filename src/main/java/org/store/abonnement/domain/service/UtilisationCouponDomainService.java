package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.UtilisationCoupon;
import org.store.abonnement.domain.repository.UtilisationCouponRepository;
import org.store.common.service.GlobalService;

@Service
public class UtilisationCouponDomainService extends GlobalService<UtilisationCoupon, UtilisationCouponRepository> {
    public UtilisationCouponDomainService(UtilisationCouponRepository repository) {
        super(repository);
    }
}
