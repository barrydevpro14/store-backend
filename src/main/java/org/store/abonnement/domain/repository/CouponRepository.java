package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.CouponResponse;
import org.store.abonnement.domain.model.Coupon;
import org.store.common.repository.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends BaseRepository<Coupon> {

    boolean existsByCode(String code);
    Optional<Coupon> findByCode(String code);

    @Query(value = """
            SELECT new org.store.abonnement.application.dto.CouponResponse(coupon)
            FROM Coupon coupon
            LEFT JOIN coupon.plan plan
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
            LEFT JOIN coupon.plan plan
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
