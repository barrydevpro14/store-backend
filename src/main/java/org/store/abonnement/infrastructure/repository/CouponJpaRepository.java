package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.repository.CouponRepository;

import java.util.UUID;

@Repository
public interface CouponJpaRepository extends JpaRepository<Coupon, UUID>, CouponRepository {
}
