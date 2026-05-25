package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.dto.CouponRequest;
import org.store.abonnement.application.dto.CouponResponse;
import org.store.abonnement.application.service.impl.CouponServiceImpl;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.CouponDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.UniqueResourceException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock private CouponDomainService couponDomainService;
    @Mock private IPlanAbonnementService planAbonnementService;
    @Mock private org.store.common.service.ValidatorService validatorService;

    @InjectMocks
    private CouponServiceImpl service;

    private UUID couponId;

    @BeforeEach
    void setUp() {
        couponId = UUID.randomUUID();
    }

    private CouponRequest validRequest() {
        return new CouponRequest(
                "PROMO10", "Réduction 10%", "POURCENTAGE",
                new BigDecimal("10"), 100,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                true, null);
    }

    private Coupon sampleCoupon() {
        Coupon coupon = new Coupon();
        coupon.setId(couponId);
        coupon.setCode("PROMO10");
        coupon.setDescription("Réduction 10%");
        coupon.setReductionType(ReductionType.POURCENTAGE);
        coupon.setValeurReduction(new BigDecimal("10"));
        coupon.setNombreUtilisationsMax(100);
        coupon.setDateDebut(LocalDate.of(2026, 1, 1));
        coupon.setDateFin(LocalDate.of(2026, 12, 31));
        coupon.setActif(true);
        return coupon;
    }

    @Test
    void create_should_persist_when_valid_and_no_plan() {
        CouponRequest request = validRequest();
        when(couponDomainService.existsByCode("PROMO10")).thenReturn(false);
        when(couponDomainService.create(request, null)).thenReturn(sampleCoupon());

        CouponResponse response = service.create(request);

        assertThat(response.code()).isEqualTo("PROMO10");
        assertThat(response.plan()).isNull();
    }

    @Test
    void create_should_resolve_plan_when_planId_provided() {
        UUID planId = UUID.randomUUID();
        PlanAbonnement plan = new PlanAbonnement();
        plan.setId(planId);
        plan.setNom("Starter");
        plan.setPrix(new BigDecimal("9900"));

        CouponRequest request = new CouponRequest(
                "PROMO_STARTER", null, "POURCENTAGE",
                new BigDecimal("10"), 10,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                true, planId);

        Coupon created = sampleCoupon();
        created.setPlan(plan);

        when(couponDomainService.existsByCode("PROMO_STARTER")).thenReturn(false);
        when(planAbonnementService.findByIdOrNull(planId)).thenReturn(plan);
        when(couponDomainService.create(request, plan)).thenReturn(created);

        CouponResponse response = service.create(request);

        assertThat(response.plan()).isNotNull();
        assertThat(response.plan().id()).isEqualTo(planId);
    }

    @Test
    void create_should_throw_when_code_taken() {
        when(couponDomainService.existsByCode("PROMO10")).thenReturn(true);

        CouponRequest req = validRequest();

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(UniqueResourceException.class);

        verify(couponDomainService, never()).create(any(), any());
    }

    @Test
    void create_should_throw_when_period_invalid() {
        CouponRequest request = new CouponRequest(
                "PROMO_X", null, "POURCENTAGE", new BigDecimal("10"), 10,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1),
                true, null);
        when(couponDomainService.existsByCode("PROMO_X")).thenReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void create_should_throw_when_reduction_inconsistent() {
        CouponRequest request = new CouponRequest(
                "PROMO_X", null, "POURCENTAGE", new BigDecimal("150"), 10,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                true, null);
        when(couponDomainService.existsByCode("PROMO_X")).thenReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void findResponseById_should_return_response() {
        when(couponDomainService.findById(couponId)).thenReturn(sampleCoupon());

        CouponResponse response = service.findResponseById(couponId);

        assertThat(response.id()).isEqualTo(couponId);
    }

    @Test
    void update_should_apply_and_save() {
        Coupon coupon = sampleCoupon();
        CouponRequest request = new CouponRequest(
                "PROMO20", "Réduction étendue", "POURCENTAGE",
                new BigDecimal("20"), 200,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31),
                true, null);

        when(couponDomainService.findById(couponId)).thenReturn(coupon);
        when(couponDomainService.existsByCode("PROMO20")).thenReturn(false);
        when(couponDomainService.applyRequest(coupon, request, null)).thenReturn(coupon);
        when(couponDomainService.save(coupon)).thenReturn(coupon);

        service.update(couponId, request);

        verify(couponDomainService).save(coupon);
    }

    @Test
    void activate_should_set_actif_true() {
        Coupon coupon = sampleCoupon();
        coupon.setActif(false);
        when(couponDomainService.findById(couponId)).thenReturn(coupon);
        when(couponDomainService.setActive(coupon, true)).thenAnswer(inv -> {
            coupon.setActif(true);
            return coupon;
        });

        CouponResponse response = service.activate(couponId);

        assertThat(response.actif()).isTrue();
    }

    @Test
    void deactivate_should_set_actif_false() {
        Coupon coupon = sampleCoupon();
        when(couponDomainService.findById(couponId)).thenReturn(coupon);
        when(couponDomainService.setActive(coupon, false)).thenAnswer(inv -> {
            coupon.setActif(false);
            return coupon;
        });

        CouponResponse response = service.deactivate(couponId);

        assertThat(response.actif()).isFalse();
    }

    @Test
    void delete_should_remove() {
        Coupon coupon = sampleCoupon();
        when(couponDomainService.findById(couponId)).thenReturn(coupon);

        service.delete(couponId);

        verify(couponDomainService).delete(coupon);
    }
}
