package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.CouponFilter;
import org.store.abonnement.application.dto.CouponResponse;
import org.store.abonnement.domain.model.Coupon;
import org.store.common.repository.BaseRepository;

import java.util.Optional;

public interface CouponRepository extends BaseRepository<Coupon> {

    boolean existsByCode(String code);

    Optional<Coupon> findByCode(String code);

    @Query(value = """
            SELECT new org.store.abonnement.application.dto.CouponResponse(coupon)
            FROM Coupon coupon
            LEFT JOIN coupon.plan plan
            WHERE (:#{#filter.code}   IS NULL OR LOWER(coupon.code) LIKE LOWER(CONCAT('%', :#{#filter.code}, '%')))
              AND (:#{#filter.actif}  IS NULL OR coupon.actif = :#{#filter.actif})
              AND (:#{#filter.planId} IS NULL OR plan.id      = :#{#filter.planId})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR coupon.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR coupon.createdAt <  :#{#filter.createdEndDateTime()})
            ORDER BY coupon.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(coupon)
            FROM Coupon coupon
            LEFT JOIN coupon.plan plan
            WHERE (:#{#filter.code}   IS NULL OR LOWER(coupon.code) LIKE LOWER(CONCAT('%', :#{#filter.code}, '%')))
              AND (:#{#filter.actif}  IS NULL OR coupon.actif = :#{#filter.actif})
              AND (:#{#filter.planId} IS NULL OR plan.id      = :#{#filter.planId})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR coupon.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR coupon.createdAt <  :#{#filter.createdEndDateTime()})
            """)
    Page<CouponResponse> findResponsesByFilter(@Param("filter") CouponFilter filter, Pageable pageable);
}
