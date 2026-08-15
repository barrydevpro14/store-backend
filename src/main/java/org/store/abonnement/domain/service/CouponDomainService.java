package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.CouponFilter;
import org.store.abonnement.application.dto.CouponRequest;
import org.store.abonnement.application.dto.CouponResponse;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.CouponRepository;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CouponDomainService extends GlobalService<Coupon, CouponRepository> {
    public CouponDomainService(CouponRepository repository) {
        super(repository);
    }

    public Coupon create(CouponRequest couponRequest, PlanAbonnement plan) {
        Coupon coupon = new Coupon();
        coupon.setNombreUtilisations(0);
        applyRequest(coupon, couponRequest, plan);
        return save(coupon);
    }

    public Coupon applyRequest(Coupon coupon, CouponRequest couponRequest, PlanAbonnement plan) {
        coupon.setCode(couponRequest.code());
        coupon.setDescription(couponRequest.description());
        coupon.setReductionType(couponRequest.reductionTypeAsEnum());
        coupon.setValeurReduction(couponRequest.valeurReduction());
        coupon.setNombreUtilisationsMax(couponRequest.nombreUtilisationsMax());
        coupon.setActif(couponRequest.actif());
        coupon.setPeriodicite(couponRequest.periodiciteAsEnum());
        coupon.setDateDebut(couponRequest.dateDebut());
        coupon.setDateFin(couponRequest.dateFin());
        coupon.setPlanAbonnement(plan);
        return coupon;
    }

    public Page<CouponResponse> findResponses(CouponFilter filter) {
        return repository.findResponsesByFilter(filter.code(), LikePatternHelper.toLikePattern(filter.code()), filter.actif(), filter.planId(), filter.startDate(), filter.endDate(), filter.toPageable());
    }

    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    public java.util.Optional<Coupon> findByCode(String code) {
        return repository.findByCode(code);
    }

    public Coupon setActive(Coupon coupon, boolean actif) {
        coupon.setActif(actif);
        return save(coupon);
    }

    public Coupon incrementUsage(Coupon coupon) {
        coupon.setNombreUtilisations(coupon.getNombreUtilisations() + 1);
        return save(coupon);
    }

    public Coupon decrementUsage(Coupon coupon) {
        if (coupon.getNombreUtilisations() > 0) {
            coupon.setNombreUtilisations(coupon.getNombreUtilisations() - 1);
            return save(coupon);
        }
        return coupon;
    }

    /** Returns the first coupon applicable to this billing cycle (plan, periodicite, date window), or empty. */
    public Optional<Coupon> findApplicable(UUID entrepriseId, UUID planId, PeriodiciteAbonnement periodicite) {
        List<Coupon> candidates = repository.findApplicableCoupons(entrepriseId, planId, periodicite, LocalDate.now());
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));
    }

    /** Auto-deactivates a coupon when its quota is exhausted. */
    public void deactivateIfExhausted(Coupon coupon) {
        if (coupon.getNombreUtilisationsMax() > 0
                && coupon.getNombreUtilisations() >= coupon.getNombreUtilisationsMax()) {
            coupon.setActif(false);
            save(coupon);
        }
    }
}
