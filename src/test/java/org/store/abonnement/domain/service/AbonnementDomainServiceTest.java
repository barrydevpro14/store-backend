package org.store.abonnement.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.AbonnementRepository;
import org.store.entreprise.domain.model.Entreprise;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbonnementDomainServiceTest {

    @Mock
    private AbonnementRepository repository;

    @InjectMocks
    private AbonnementDomainService service;

    @Test
    void createTrial_should_build_abonnement_with_30_days_and_active_status() {
        Entreprise entreprise = new Entreprise();
        PlanAbonnement plan = new PlanAbonnement();
        when(repository.save(any(Abonnement.class))).thenAnswer(inv -> inv.getArgument(0));

        Abonnement result = service.createTrial(entreprise, plan);

        ArgumentCaptor<Abonnement> captor = ArgumentCaptor.forClass(Abonnement.class);
        verify(repository).save(captor.capture());
        Abonnement saved = captor.getValue();
        assertThat(saved.getEntreprise()).isSameAs(entreprise);
        assertThat(saved.getPlan()).isSameAs(plan);
        assertThat(saved.getDateDebut()).isEqualTo(LocalDate.now());
        assertThat(saved.getDateFin()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(saved.isActif()).isTrue();
        assertThat(saved.isRenouvellementAuto()).isFalse();
        assertThat(saved.getStatut()).isEqualTo(AbonnementStatut.ACTIF);
        assertThat(result).isSameAs(saved);
    }
}
