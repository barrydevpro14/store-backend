package org.store.abonnement.domain.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.domain.model.UtilisationCoupon;
import org.store.common.repository.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface UtilisationCouponRepository extends BaseRepository<UtilisationCoupon> {

    @Query("""
            SELECT utilisation.coupon.id
            FROM UtilisationCoupon utilisation
            WHERE utilisation.abonnement.id = :abonnementId
            """)
    Optional<UUID> findCouponIdByAbonnementId(@Param("abonnementId") UUID abonnementId);

    @Modifying
    @Query("""
            DELETE FROM UtilisationCoupon utilisation
            WHERE utilisation.abonnement.id = :abonnementId
            """)
    void deleteByAbonnementId(@Param("abonnementId") UUID abonnementId);
}
