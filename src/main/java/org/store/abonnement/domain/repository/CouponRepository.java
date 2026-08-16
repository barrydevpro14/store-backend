package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.CouponResponse;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.common.repository.BaseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends BaseRepository<Coupon> {

    boolean existsByCode(String code);
    Optional<Coupon> findByCode(String code);

    /**
     * Finds the first coupon applicable to a given billing cycle:
     * active, targeting this enterprise or global (entreprise IS NULL),
     * matching this plan or global (plan IS NULL), quota not exhausted
     * (nombreUtilisationsMax = 0 means unlimited).
     */
    @Query("""
            SELECT c FROM Coupon c
            WHERE c.actif = true
              AND (c.entreprise IS NULL OR c.entreprise.id = :entrepriseId)
              AND (c.planAbonnement IS NULL OR c.planAbonnement.id = :planId)
              AND (c.periodicite IS NULL OR c.periodicite = :periodicite)
              AND :today BETWEEN c.dateDebut AND c.dateFin
              AND (c.nombreUtilisationsMax = 0
                   OR c.nombreUtilisations < c.nombreUtilisationsMax)
            ORDER BY c.createdAt ASC
            """)
    List<Coupon> findApplicableCoupons(@Param("entrepriseId") UUID entrepriseId,
                                       @Param("planId") UUID planId,
                                       @Param("periodicite") PeriodiciteAbonnement periodicite,
                                       @Param("today") LocalDate today);

    /** Finds global coupons (entreprise IS NULL) applicable for a given plan + periodicite, quota not exhausted. */
    @Query("""
            SELECT c FROM Coupon c
            WHERE c.actif = true
              AND c.entreprise IS NULL
              AND (c.planAbonnement IS NULL OR c.planAbonnement.id = :planId)
              AND (c.periodicite IS NULL OR c.periodicite = :periodicite)
              AND :today BETWEEN c.dateDebut AND c.dateFin
              AND (c.nombreUtilisationsMax = 0
                   OR c.nombreUtilisations < c.nombreUtilisationsMax)
            ORDER BY c.createdAt ASC
            """)
    List<Coupon> findApplicableGlobalCoupons(@Param("planId") UUID planId,
                                             @Param("periodicite") PeriodiciteAbonnement periodicite,
                                             @Param("today") LocalDate today);

    @Query(value = """
            SELECT new org.store.abonnement.application.dto.CouponResponse(coupon)
            FROM Coupon coupon
            LEFT JOIN coupon.planAbonnement plan
            WHERE (:code IS NULL OR :code = '' OR LOWER(coupon.code) LIKE :codePattern)
              AND (:actif IS NULL OR coupon.actif = :actif)
              AND (:planId IS NULL OR plan.id = :planId)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', coupon.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', coupon.createdAt) <= CAST(:endDate AS date))
            ORDER BY coupon.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(coupon)
            FROM Coupon coupon
            LEFT JOIN coupon.planAbonnement plan
            WHERE (:code IS NULL OR :code = '' OR LOWER(coupon.code) LIKE :codePattern)
              AND (:actif IS NULL OR coupon.actif = :actif)
              AND (:planId IS NULL OR plan.id = :planId)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', coupon.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', coupon.createdAt) <= CAST(:endDate AS date))
            """)
    Page<CouponResponse> findResponsesByFilter(
            @Param("code") String code,
            @Param("codePattern") String codePattern,
            @Param("actif") Boolean actif,
            @Param("planId") UUID planId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);
}
