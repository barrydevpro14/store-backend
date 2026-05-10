package org.store.abonnement.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.Coupon;

import java.util.UUID;

public interface CouponJpaRepository extends JpaRepository<Coupon, UUID> {
}
