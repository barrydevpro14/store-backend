package org.store.abonnement.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.TypePlanAbonnement;
import org.store.abonnement.domain.repository.TypePlanAbonnementRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TypePlanAbonnementDomainServiceTest {

    @Mock private TypePlanAbonnementRepository repository;

    @InjectMocks
    private TypePlanAbonnementDomainService service;

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
    }

    private SubscriptionTypeRequest validRequest() {
        return new SubscriptionTypeRequest(
                "Annuel", 12, "POURCENTAGE", new BigDecimal("15"),
                true, true, 20);
    }

    @Test
    void create_should_attach_plan_apply_request_and_save() {
        SubscriptionTypeRequest request = validRequest();
        when(repository.save(any(TypePlanAbonnement.class))).thenAnswer(inv -> inv.getArgument(0));

        TypePlanAbonnement result = service.create(plan, request);

        ArgumentCaptor<TypePlanAbonnement> captor = ArgumentCaptor.forClass(TypePlanAbonnement.class);
        verify(repository).save(captor.capture());
        TypePlanAbonnement saved = captor.getValue();
        assertThat(saved.getPlan()).isSameAs(plan);
        assertThat(saved.getNom()).isEqualTo("Annuel");
        assertThat(saved.getDureeMois()).isEqualTo(12);
        assertThat(saved.getReductionType()).isEqualTo(ReductionType.POURCENTAGE);
        assertThat(saved.getValeurReduction()).isEqualByComparingTo("15");
        assertThat(saved.isRecommande()).isTrue();
        assertThat(saved.isActif()).isTrue();
        assertThat(saved.getOrdre()).isEqualTo(20);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void applyRequest_should_copy_request_fields_onto_entity_without_touching_plan() {
        TypePlanAbonnement type = new TypePlanAbonnement();
        type.setPlan(plan);
        type.setNom("Old");
        type.setDureeMois(1);

        TypePlanAbonnement result = service.applyRequest(type, validRequest());

        assertThat(result).isSameAs(type);
        assertThat(type.getPlan()).isSameAs(plan);
        assertThat(type.getNom()).isEqualTo("Annuel");
        assertThat(type.getDureeMois()).isEqualTo(12);
        assertThat(type.getReductionType()).isEqualTo(ReductionType.POURCENTAGE);
        assertThat(type.getValeurReduction()).isEqualByComparingTo("15");
        assertThat(type.isRecommande()).isTrue();
        assertThat(type.isActif()).isTrue();
        assertThat(type.getOrdre()).isEqualTo(20);
    }

    @Test
    void applyRequest_should_clear_reduction_when_request_has_null_values() {
        TypePlanAbonnement type = new TypePlanAbonnement();
        type.setReductionType(ReductionType.POURCENTAGE);
        type.setValeurReduction(new BigDecimal("10"));
        SubscriptionTypeRequest request = new SubscriptionTypeRequest(
                "Mensuel", 1, null, null, false, true, 0);

        service.applyRequest(type, request);

        assertThat(type.getReductionType()).isNull();
        assertThat(type.getValeurReduction()).isNull();
    }

    @Test
    void findResponses_should_delegate_with_planId_filter_and_pageable() {
        SubscriptionTypeFilter filter = new SubscriptionTypeFilter("Annuel", true, null, null, 0, 10);
        Page<SubscriptionTypeResponse> page = new PageImpl<>(List.of());
        when(repository.findResponsesByFilter(eq(planId), eq(filter), any(Pageable.class)))
                .thenReturn(page);

        Page<SubscriptionTypeResponse> result = service.findResponses(planId, filter);

        assertThat(result).isSameAs(page);
    }

    @Test
    void findActifResponsesByPlanId_should_delegate() {
        List<SubscriptionTypeResponse> expected = List.of();
        when(repository.findActifResponsesByPlanId(planId)).thenReturn(expected);

        assertThat(service.findActifResponsesByPlanId(planId)).isSameAs(expected);
    }

    @Test
    void existsByPlanIdAndNom_should_return_true_when_repository_says_so() {
        when(repository.existsByPlanIdAndNom(planId, "Annuel")).thenReturn(true);

        assertThat(service.existsByPlanIdAndNom(planId, "Annuel")).isTrue();
    }

    @Test
    void existsByPlanIdAndNom_should_return_false_when_repository_says_no() {
        when(repository.existsByPlanIdAndNom(planId, "Annuel")).thenReturn(false);

        assertThat(service.existsByPlanIdAndNom(planId, "Annuel")).isFalse();
    }

    @Test
    void existsByPlanIdAndNomAndIdNot_should_delegate_with_id_exclusion() {
        when(repository.existsByPlanIdAndNomAndIdNot(planId, "Annuel", typeId)).thenReturn(true);

        assertThat(service.existsByPlanIdAndNomAndIdNot(planId, "Annuel", typeId)).isTrue();
    }

    @Test
    void setActive_should_force_true_and_persist() {
        TypePlanAbonnement type = new TypePlanAbonnement();
        type.setActif(false);
        when(repository.save(type)).thenAnswer(inv -> inv.getArgument(0));

        TypePlanAbonnement result = service.setActive(type, true);

        assertThat(result.isActif()).isTrue();
        verify(repository).save(type);
    }

    @Test
    void setActive_should_force_false_and_persist() {
        TypePlanAbonnement type = new TypePlanAbonnement();
        type.setActif(true);
        when(repository.save(type)).thenAnswer(inv -> inv.getArgument(0));

        TypePlanAbonnement result = service.setActive(type, false);

        assertThat(result.isActif()).isFalse();
        verify(repository).save(type);
    }
}
