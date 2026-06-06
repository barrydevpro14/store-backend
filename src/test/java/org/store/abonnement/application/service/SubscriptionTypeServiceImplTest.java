package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.application.service.impl.SubscriptionTypeServiceImpl;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.TypePlanAbonnement;
import org.store.abonnement.domain.service.TypePlanAbonnementDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.UniqueResourceException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionTypeServiceImplTest {

    @Mock private TypePlanAbonnementDomainService typePlanAbonnementDomainService;
    @Mock private IPlanAbonnementService planAbonnementService;
    @Mock private org.store.common.service.ValidatorService validatorService;

    @InjectMocks
    private SubscriptionTypeServiceImpl service;

    private UUID planId;
    private UUID typeId;
    private PlanAbonnement plan;

    @BeforeEach
    void setUp() {
        planId = UUID.randomUUID();
        typeId = UUID.randomUUID();
        plan = new PlanAbonnement();
        plan.setId(planId);
        plan.setNom("Pro");
        plan.setPrix(new BigDecimal("9900"));
    }

    private SubscriptionTypeRequest validRequest() {
        return new SubscriptionTypeRequest(
                "Mensuel", 1, "POURCENTAGE", new BigDecimal("0"),
                false, true, 10);
    }

    private TypePlanAbonnement sampleType() {
        TypePlanAbonnement type = new TypePlanAbonnement();
        type.setId(typeId);
        type.setPlan(plan);
        type.setNom("Mensuel");
        type.setDureeMois(1);
        type.setReductionType(ReductionType.POURCENTAGE);
        type.setValeurReduction(new BigDecimal("0"));
        type.setRecommande(false);
        type.setActif(true);
        type.setOrdre(10);
        return type;
    }

    @Test
    void create_should_persist_when_nom_available_and_reduction_valid() {
        SubscriptionTypeRequest request = validRequest();
        when(planAbonnementService.findById(planId)).thenReturn(plan);
        when(typePlanAbonnementDomainService.existsByPlanIdAndNom(planId, "Mensuel")).thenReturn(false);
        when(typePlanAbonnementDomainService.create(plan, request)).thenReturn(sampleType());

        SubscriptionTypeResponse response = service.create(planId, request);

        assertThat(response.nom()).isEqualTo("Mensuel");
        assertThat(response.planId()).isEqualTo(planId);
        assertThat(response.planNom()).isEqualTo("Pro");
    }

    @Test
    void create_should_throw_when_nom_taken_in_plan() {
        when(planAbonnementService.findById(planId)).thenReturn(plan);
        when(typePlanAbonnementDomainService.existsByPlanIdAndNom(planId, "Mensuel")).thenReturn(true);

        SubscriptionTypeRequest req = validRequest();

        assertThatThrownBy(() -> service.create(planId, req))
                .isInstanceOf(UniqueResourceException.class);

        verify(typePlanAbonnementDomainService, never()).create(any(), any());
    }

    @Test
    void create_should_throw_when_reduction_type_without_value() {
        SubscriptionTypeRequest request = new SubscriptionTypeRequest(
                "Mensuel", 1, "POURCENTAGE", null, false, true, 0);
        when(planAbonnementService.findById(planId)).thenReturn(plan);
        when(typePlanAbonnementDomainService.existsByPlanIdAndNom(planId, "Mensuel")).thenReturn(false);

        assertThatThrownBy(() -> service.create(planId, request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void create_should_throw_when_percentage_above_100() {
        SubscriptionTypeRequest request = new SubscriptionTypeRequest(
                "Annuel", 12, "POURCENTAGE", new BigDecimal("150"), false, true, 0);
        when(planAbonnementService.findById(planId)).thenReturn(plan);
        when(typePlanAbonnementDomainService.existsByPlanIdAndNom(planId, "Annuel")).thenReturn(false);

        assertThatThrownBy(() -> service.create(planId, request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void create_should_throw_when_value_without_reduction_type() {
        SubscriptionTypeRequest request = new SubscriptionTypeRequest(
                "Annuel", 12, null, new BigDecimal("10"), false, true, 0);
        when(planAbonnementService.findById(planId)).thenReturn(plan);
        when(typePlanAbonnementDomainService.existsByPlanIdAndNom(planId, "Annuel")).thenReturn(false);

        assertThatThrownBy(() -> service.create(planId, request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void findResponseById_should_return_response() {
        when(typePlanAbonnementDomainService.findById(typeId)).thenReturn(sampleType());

        SubscriptionTypeResponse response = service.findResponseById(planId, typeId);

        assertThat(response.id()).isEqualTo(typeId);
    }

    @Test
    void findResponseById_should_throw_when_type_belongs_to_other_plan() {
        TypePlanAbonnement type = sampleType();
        PlanAbonnement otherPlan = new PlanAbonnement();
        otherPlan.setId(UUID.randomUUID());
        type.setPlan(otherPlan);
        when(typePlanAbonnementDomainService.findById(typeId)).thenReturn(type);

        assertThatThrownBy(() -> service.findResponseById(planId, typeId))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void findAll_should_delegate() {
        SubscriptionTypeFilter filter = new SubscriptionTypeFilter(null, null, null, null, null, 0, 10);
        Page<SubscriptionTypeResponse> page = new PageImpl<>(List.of());
        when(planAbonnementService.findById(planId)).thenReturn(plan);
        when(typePlanAbonnementDomainService.findResponses(planId, filter)).thenReturn(page);

        assertThat(service.findAll(planId, filter)).isSameAs(page);
    }

    @Test
    void update_should_apply_and_save() {
        TypePlanAbonnement type = sampleType();
        SubscriptionTypeRequest request = new SubscriptionTypeRequest(
                "Annuel", 12, "POURCENTAGE", new BigDecimal("15"),
                true, true, 20);

        when(typePlanAbonnementDomainService.findById(typeId)).thenReturn(type);
        when(typePlanAbonnementDomainService.existsByPlanIdAndNomAndIdNot(planId, "Annuel", typeId))
                .thenReturn(false);
        when(typePlanAbonnementDomainService.applyRequest(type, request)).thenReturn(type);
        when(typePlanAbonnementDomainService.save(type)).thenReturn(type);

        service.update(planId, typeId, request);

        verify(typePlanAbonnementDomainService).applyRequest(type, request);
        verify(typePlanAbonnementDomainService).save(type);
    }

    @Test
    void update_should_skip_unicity_when_nom_unchanged() {
        TypePlanAbonnement type = sampleType();
        SubscriptionTypeRequest request = validRequest();
        when(typePlanAbonnementDomainService.findById(typeId)).thenReturn(type);
        when(typePlanAbonnementDomainService.applyRequest(type, request)).thenReturn(type);
        when(typePlanAbonnementDomainService.save(type)).thenReturn(type);

        service.update(planId, typeId, request);

        verify(typePlanAbonnementDomainService, never())
                .existsByPlanIdAndNomAndIdNot(eq(planId), any(), any());
    }

    @Test
    void activate_should_set_actif_true() {
        TypePlanAbonnement type = sampleType();
        type.setActif(false);
        when(typePlanAbonnementDomainService.findById(typeId)).thenReturn(type);
        when(typePlanAbonnementDomainService.setActive(type, true)).thenAnswer(inv -> {
            type.setActif(true);
            return type;
        });

        SubscriptionTypeResponse response = service.activate(planId, typeId);

        assertThat(response.actif()).isTrue();
    }

    @Test
    void deactivate_should_set_actif_false() {
        TypePlanAbonnement type = sampleType();
        when(typePlanAbonnementDomainService.findById(typeId)).thenReturn(type);
        when(typePlanAbonnementDomainService.setActive(type, false)).thenAnswer(inv -> {
            type.setActif(false);
            return type;
        });

        SubscriptionTypeResponse response = service.deactivate(planId, typeId);

        assertThat(response.actif()).isFalse();
    }

    @Test
    void delete_should_remove() {
        TypePlanAbonnement type = sampleType();
        when(typePlanAbonnementDomainService.findById(typeId)).thenReturn(type);

        service.delete(planId, typeId);

        verify(typePlanAbonnementDomainService).delete(type);
    }
}
