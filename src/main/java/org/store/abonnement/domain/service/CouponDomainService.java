package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.CouponFilter;
import org.store.abonnement.application.dto.CouponRequest;
import org.store.abonnement.application.dto.CouponResponse;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.CouponRepository;
import org.store.common.service.GlobalService;

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
        coupon.setDateDebut(couponRequest.dateDebut());
        coupon.setDateFin(couponRequest.dateFin());
        coupon.setActif(couponRequest.actif());
        coupon.setPlan(plan);
        return coupon;
    }

    public Page<CouponResponse> findResponses(CouponFilter filter) {
        return repository.findResponsesByFilter(filter, filter.toPageable());
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
}
