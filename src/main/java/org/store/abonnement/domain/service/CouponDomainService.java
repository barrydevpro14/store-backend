package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.repository.CouponRepository;
import org.store.common.service.GlobalService;

@Service
public class CouponDomainService extends GlobalService<Coupon, CouponRepository> {
    public CouponDomainService(CouponRepository repository) {
        super(repository);
    }
}
