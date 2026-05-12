package org.store.abonnement.application.service;

import org.store.abonnement.application.service.impl.AbonnementServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.entreprise.domain.model.Entreprise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbonnementServiceImplTest {

    @Mock
    private AbonnementDomainService abonnementDomainService;

    @InjectMocks
    private AbonnementServiceImpl service;

    @Test
    void createTrial_should_delegate_to_domain_service() {
        Entreprise entreprise = new Entreprise();
        PlanAbonnement plan = new PlanAbonnement();
        Abonnement expected = new Abonnement();
        when(abonnementDomainService.createTrial(entreprise, plan)).thenReturn(expected);

        Abonnement result = service.createTrial(entreprise, plan);

        assertThat(result).isSameAs(expected);
    }
}
