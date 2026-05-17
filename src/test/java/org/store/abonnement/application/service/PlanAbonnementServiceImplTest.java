package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.store.abonnement.application.dto.PlanAbonnementFilter;
import org.store.abonnement.application.dto.PlanAbonnementRequest;
import org.store.abonnement.application.dto.PlanAbonnementResponse;
import org.store.abonnement.application.service.impl.PlanAbonnementServiceImpl;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.UniqueResourceException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanAbonnementServiceImplTest {

    @Mock private PlanAbonnementDomainService planAbonnementDomainService;
    @Mock private org.store.common.service.ValidatorService validatorService;

    @InjectMocks
    private PlanAbonnementServiceImpl service;

    private UUID planId;

    @BeforeEach
    void setUp() {
        planId = UUID.randomUUID();
    }

    private PlanAbonnementRequest sampleRequest() {
        return new PlanAbonnementRequest(
                "Starter",
                "Plan d'entrée de gamme",
                new BigDecimal("9900.00"),
                1,
                3,
                true,
                true,
                true,
                false,
                true,
                true,
                false,
                10
        );
    }

    private PlanAbonnement samplePlan() {
        PlanAbonnement plan = new PlanAbonnement();
        plan.setId(planId);
        plan.setNom("Starter");
        plan.setDescription("Plan d'entrée de gamme");
        plan.setPrix(new BigDecimal("9900.00"));
        plan.setNombreMagasinsMax(1);
        plan.setNombreEmployesMax(3);
        plan.setGestionStock(true);
        plan.setGestionVente(true);
        plan.setGestionAchat(true);
        plan.setGestionComptabilite(false);
        plan.setActif(true);
        plan.setVisible(true);
        plan.setTrial(false);
        plan.setOrdre(10);
        return plan;
    }

    @Test
    void create_should_persist_when_nom_available() {
        PlanAbonnementRequest request = sampleRequest();
        PlanAbonnement created = samplePlan();

        when(planAbonnementDomainService.existsByNom("Starter")).thenReturn(false);
        when(planAbonnementDomainService.create(request)).thenReturn(created);

        PlanAbonnementResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(planId);
        assertThat(response.nom()).isEqualTo("Starter");
        assertThat(response.prix()).isEqualByComparingTo("9900.00");
        assertThat(response.actif()).isTrue();
    }

    @Test
    void create_should_throw_when_nom_taken() {
        PlanAbonnementRequest request = sampleRequest();
        when(planAbonnementDomainService.existsByNom("Starter")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UniqueResourceException.class);

        verify(planAbonnementDomainService, never()).create(any());
    }

    @Test
    void findFirstTrialActif_should_return_when_present() {
        PlanAbonnement trial = samplePlan();
        trial.setTrial(true);
        when(planAbonnementDomainService.findFirstTrialActif()).thenReturn(Optional.of(trial));

        assertThat(service.findFirstTrialActif()).isSameAs(trial);
    }

    @Test
    void findFirstTrialActif_should_throw_when_absent() {
        when(planAbonnementDomainService.findFirstTrialActif()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findFirstTrialActif())
                .isInstanceOf(EntityException.class);
    }

    @Test
    void findResponseById_should_return_response() {
        when(planAbonnementDomainService.findById(planId)).thenReturn(samplePlan());

        PlanAbonnementResponse response = service.findResponseById(planId);

        assertThat(response.id()).isEqualTo(planId);
        assertThat(response.nom()).isEqualTo("Starter");
    }

    @Test
    void findAll_should_delegate_to_domain_service() {
        PlanAbonnementFilter filter = new PlanAbonnementFilter(null, null, null, null, 0, 10);
        PlanAbonnementResponse sample = new PlanAbonnementResponse(planId, "Starter", null,
                new BigDecimal("9900"), 1, 3, true, true, true, false, true, true, false, 10);
        Page<PlanAbonnementResponse> page = new PageImpl<>(List.of(sample));

        when(planAbonnementDomainService.findResponses(filter)).thenReturn(page);

        Page<PlanAbonnementResponse> result = service.findAll(filter);

        assertThat(result.getContent()).containsExactly(sample);
    }

    @Test
    void update_should_apply_request_and_save() {
        PlanAbonnement plan = samplePlan();
        PlanAbonnementRequest request = new PlanAbonnementRequest(
                "Premium", "Premium gamme", new BigDecimal("29900"),
                5, 20, true, true, true, true, true, true, false, 20);

        when(planAbonnementDomainService.findById(planId)).thenReturn(plan);
        when(planAbonnementDomainService.existsByNom("Premium")).thenReturn(false);
        when(planAbonnementDomainService.applyRequest(plan, request)).thenReturn(plan);
        when(planAbonnementDomainService.save(plan)).thenReturn(plan);

        service.update(planId, request);

        verify(planAbonnementDomainService).applyRequest(plan, request);
        verify(planAbonnementDomainService).save(plan);
    }

    @Test
    void update_should_skip_unicity_when_nom_unchanged() {
        PlanAbonnement plan = samplePlan();
        PlanAbonnementRequest request = sampleRequest();

        when(planAbonnementDomainService.findById(planId)).thenReturn(plan);
        when(planAbonnementDomainService.applyRequest(plan, request)).thenReturn(plan);
        when(planAbonnementDomainService.save(plan)).thenReturn(plan);

        service.update(planId, request);

        verify(planAbonnementDomainService, never()).existsByNom(any());
    }

    @Test
    void update_should_throw_when_new_nom_taken() {
        PlanAbonnement plan = samplePlan();
        PlanAbonnementRequest request = new PlanAbonnementRequest(
                "Premium", null, new BigDecimal("100"), 1, 1,
                true, true, true, false, true, true, false, 0);

        when(planAbonnementDomainService.findById(planId)).thenReturn(plan);
        when(planAbonnementDomainService.existsByNom("Premium")).thenReturn(true);

        assertThatThrownBy(() -> service.update(planId, request))
                .isInstanceOf(UniqueResourceException.class);

        verify(planAbonnementDomainService, never()).save(any());
    }

    @Test
    void activate_should_set_actif_true() {
        PlanAbonnement plan = samplePlan();
        plan.setActif(false);
        when(planAbonnementDomainService.findById(planId)).thenReturn(plan);
        when(planAbonnementDomainService.setActive(plan, true)).thenAnswer(inv -> {
            plan.setActif(true);
            return plan;
        });

        PlanAbonnementResponse response = service.activate(planId);

        assertThat(response.actif()).isTrue();
    }

    @Test
    void deactivate_should_set_actif_false() {
        PlanAbonnement plan = samplePlan();
        when(planAbonnementDomainService.findById(planId)).thenReturn(plan);
        when(planAbonnementDomainService.setActive(plan, false)).thenAnswer(inv -> {
            plan.setActif(false);
            return plan;
        });

        PlanAbonnementResponse response = service.deactivate(planId);

        assertThat(response.actif()).isFalse();
    }

    @Test
    void delete_should_remove_plan() {
        PlanAbonnement plan = samplePlan();
        when(planAbonnementDomainService.findById(planId)).thenReturn(plan);

        service.delete(planId);

        verify(planAbonnementDomainService).delete(plan);
    }

    @Test
    void ensureNomAvailable_should_throw_when_taken() {
        when(planAbonnementDomainService.existsByNom("Starter")).thenReturn(true);

        assertThatThrownBy(() -> service.ensureNomAvailable("Starter"))
                .isInstanceOf(UniqueResourceException.class);
    }
}
