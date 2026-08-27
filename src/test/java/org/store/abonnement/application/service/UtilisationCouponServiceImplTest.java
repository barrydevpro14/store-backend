package org.store.abonnement.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.service.impl.UtilisationCouponServiceImpl;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.UtilisationCoupon;
import org.store.abonnement.domain.service.UtilisationCouponDomainService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilisationCouponServiceImplTest {

    @Mock private UtilisationCouponDomainService utilisationCouponDomainService;

    @InjectMocks
    private UtilisationCouponServiceImpl service;

    @Test
    void createWithPaiement_should_delegate_to_domain_service() {
        Coupon coupon = new Coupon();
        Abonnement abonnement = new Abonnement();
        PaiementAbonnement paiement = new PaiementAbonnement();
        UtilisationCoupon expected = new UtilisationCoupon();

        when(utilisationCouponDomainService.createWithPaiement(coupon, abonnement, paiement)).thenReturn(expected);

        UtilisationCoupon result = service.createWithPaiement(coupon, abonnement, paiement);

        assertThat(result).isSameAs(expected);
        verify(utilisationCouponDomainService).createWithPaiement(coupon, abonnement, paiement);
    }
}
