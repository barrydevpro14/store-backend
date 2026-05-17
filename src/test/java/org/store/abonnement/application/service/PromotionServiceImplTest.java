package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.dto.PromotionRequest;
import org.store.abonnement.application.dto.PromotionResponse;
import org.store.abonnement.application.service.impl.PromotionServiceImpl;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.service.PromotionDomainService;
import org.store.common.exceptions.BadArgumentException;

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
class PromotionServiceImplTest {

    @Mock private PromotionDomainService promotionDomainService;
    @Mock private IPlanAbonnementService planAbonnementService;
    @Mock private org.store.common.service.ValidatorService validatorService;

    @InjectMocks
    private PromotionServiceImpl service;

    private UUID promotionId;

    @BeforeEach
    void setUp() {
        promotionId = UUID.randomUUID();
    }

    private PromotionRequest validRequest() {
        return new PromotionRequest(
                "Rentrée 2026", "Promo rentrée",
                "POURCENTAGE", new BigDecimal("15"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                true, null);
    }

    private Promotion samplePromotion() {
        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setNom("Rentrée 2026");
        promotion.setReductionType(ReductionType.POURCENTAGE);
        promotion.setValeurReduction(new BigDecimal("15"));
        promotion.setDateDebut(LocalDate.of(2026, 9, 1));
        promotion.setDateFin(LocalDate.of(2026, 9, 30));
        promotion.setActif(true);
        return promotion;
    }

    @Test
    void create_should_persist_when_valid() {
        PromotionRequest request = validRequest();
        when(promotionDomainService.create(request, null)).thenReturn(samplePromotion());

        PromotionResponse response = service.create(request);

        assertThat(response.nom()).isEqualTo("Rentrée 2026");
        assertThat(response.plan()).isNull();
    }

    @Test
    void create_should_resolve_plan() {
        UUID planId = UUID.randomUUID();
        PlanAbonnement plan = new PlanAbonnement();
        plan.setId(planId);
        plan.setNom("Premium");
        plan.setPrix(new BigDecimal("29900"));

        PromotionRequest request = new PromotionRequest(
                "Pro promo", null, "POURCENTAGE", new BigDecimal("20"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                true, planId);

        Promotion created = samplePromotion();
        created.setPlan(plan);

        when(planAbonnementService.findByIdOrNull(planId)).thenReturn(plan);
        when(promotionDomainService.create(request, plan)).thenReturn(created);

        PromotionResponse response = service.create(request);

        assertThat(response.plan()).isNotNull();
        assertThat(response.plan().id()).isEqualTo(planId);
    }

    @Test
    void create_should_throw_when_period_invalid() {
        PromotionRequest request = new PromotionRequest(
                "X", null, "POURCENTAGE", new BigDecimal("10"),
                LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1),
                true, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadArgumentException.class);

        verify(promotionDomainService, never()).create(any(), any());
    }

    @Test
    void create_should_throw_when_reduction_inconsistent() {
        PromotionRequest request = new PromotionRequest(
                "X", null, "POURCENTAGE", new BigDecimal("150"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                true, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void findResponseById_should_return_response() {
        when(promotionDomainService.findById(promotionId)).thenReturn(samplePromotion());

        PromotionResponse response = service.findResponseById(promotionId);

        assertThat(response.id()).isEqualTo(promotionId);
    }

    @Test
    void update_should_apply_and_save() {
        Promotion promotion = samplePromotion();
        PromotionRequest request = validRequest();

        when(promotionDomainService.findById(promotionId)).thenReturn(promotion);
        when(promotionDomainService.applyRequest(promotion, request, null)).thenReturn(promotion);
        when(promotionDomainService.save(promotion)).thenReturn(promotion);

        service.update(promotionId, request);

        verify(promotionDomainService).save(promotion);
    }

    @Test
    void activate_should_set_actif_true() {
        Promotion promotion = samplePromotion();
        promotion.setActif(false);
        when(promotionDomainService.findById(promotionId)).thenReturn(promotion);
        when(promotionDomainService.setActive(promotion, true)).thenAnswer(inv -> {
            promotion.setActif(true);
            return promotion;
        });

        PromotionResponse response = service.activate(promotionId);

        assertThat(response.actif()).isTrue();
    }

    @Test
    void deactivate_should_set_actif_false() {
        Promotion promotion = samplePromotion();
        when(promotionDomainService.findById(promotionId)).thenReturn(promotion);
        when(promotionDomainService.setActive(promotion, false)).thenAnswer(inv -> {
            promotion.setActif(false);
            return promotion;
        });

        PromotionResponse response = service.deactivate(promotionId);

        assertThat(response.actif()).isFalse();
    }

    @Test
    void delete_should_remove() {
        Promotion promotion = samplePromotion();
        when(promotionDomainService.findById(promotionId)).thenReturn(promotion);

        service.delete(promotionId);

        verify(promotionDomainService).delete(promotion);
    }
}
