package org.store.abonnement.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;
import org.store.common.exceptions.EntityException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanAbonnementServiceImplTest {

    @Mock
    private PlanAbonnementDomainService planAbonnementDomainService;

    @InjectMocks
    private PlanAbonnementServiceImpl service;

    @Test
    void should_return_plan_when_trial_actif_exists() {
        PlanAbonnement plan = new PlanAbonnement();
        when(planAbonnementDomainService.findFirstTrialActif()).thenReturn(Optional.of(plan));

        PlanAbonnement result = service.findFirstTrialActif();

        assertThat(result).isSameAs(plan);
    }

    @Test
    void should_throw_entity_exception_when_no_trial_plan_actif() {
        when(planAbonnementDomainService.findFirstTrialActif()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findFirstTrialActif())
                .isInstanceOf(EntityException.class);
    }
}
