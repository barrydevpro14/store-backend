package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.model.Coupon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        String code,
        String description,
        ReductionType reductionType,
        BigDecimal valeurReduction,
        int nombreUtilisationsMax,
        int nombreUtilisations,
        LocalDate dateDebut,
        LocalDate dateFin,
        boolean actif,
        PlanAbonnementSummaryResponse plan
) {
    public CouponResponse(Coupon coupon) {
        this(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getReductionType(),
                coupon.getValeurReduction(),
                coupon.getNombreUtilisationsMax(),
                coupon.getNombreUtilisations(),
                coupon.getDateDebut(),
                coupon.getDateFin(),
                coupon.isActif(),
                coupon.getPlan() == null ? null : new PlanAbonnementSummaryResponse(coupon.getPlan())
        );
    }
}
