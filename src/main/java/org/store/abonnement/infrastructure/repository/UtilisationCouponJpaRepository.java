package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.UtilisationCoupon;
import org.store.abonnement.domain.repository.UtilisationCouponRepository;

import java.util.UUID;

public interface UtilisationCouponJpaRepository extends JpaRepository<UtilisationCoupon, UUID>, UtilisationCouponRepository {
}
