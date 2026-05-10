package org.store.abonnement.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.UtilisationCoupon;

import java.util.UUID;

public interface UtilisationCouponJpaRepository extends JpaRepository<UtilisationCoupon, UUID> {
}
