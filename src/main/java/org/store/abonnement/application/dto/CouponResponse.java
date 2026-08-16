package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
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
        boolean actif,
        PeriodiciteAbonnement periodicite,
        LocalDate dateDebut,
        LocalDate dateFin,
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
                coupon.isActif(),
                coupon.getPeriodicite(),
                coupon.getDateDebut(),
                coupon.getDateFin(),
                coupon.getPlanAbonnement() == null ? null : new PlanAbonnementSummaryResponse(coupon.getPlanAbonnement())
        );
    }
}
