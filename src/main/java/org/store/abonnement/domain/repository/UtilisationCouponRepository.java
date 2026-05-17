package org.store.abonnement.domain.repository;

import org.store.abonnement.domain.model.UtilisationCoupon;
import org.store.common.repository.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface UtilisationCouponRepository extends BaseRepository<UtilisationCoupon> {

    Optional<UtilisationCoupon> findByAbonnementId(UUID abonnementId);
}
