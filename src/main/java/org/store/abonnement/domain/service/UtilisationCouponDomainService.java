package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.UtilisationCoupon;
import org.store.abonnement.domain.repository.UtilisationCouponRepository;
import org.store.common.service.GlobalService;
import org.store.entreprise.domain.model.Entreprise;

import java.util.Optional;
import java.util.UUID;

@Service
public class UtilisationCouponDomainService extends GlobalService<UtilisationCoupon, UtilisationCouponRepository> {
    public UtilisationCouponDomainService(UtilisationCouponRepository repository) {
        super(repository);
    }

    public UtilisationCoupon create(Coupon coupon, Entreprise entreprise, Abonnement abonnement) {
        UtilisationCoupon utilisation = new UtilisationCoupon();
        utilisation.setCoupon(coupon);
        utilisation.setEntreprise(entreprise);
        utilisation.setAbonnement(abonnement);
        return save(utilisation);
    }

    public Optional<UtilisationCoupon> findByAbonnementId(UUID abonnementId) {
        return repository.findByAbonnementId(abonnementId);
    }
}
