package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.repository.CouponRepository;

import java.util.UUID;

public interface CouponJpaRepository extends JpaRepository<Coupon, UUID>, CouponRepository {
}
