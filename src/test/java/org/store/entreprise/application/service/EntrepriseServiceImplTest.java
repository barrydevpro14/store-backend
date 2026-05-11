package org.store.entreprise.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.repository.EntrepriseRepository;
import org.store.users.domain.model.Proprietaire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntrepriseServiceImplTest {

    @Mock
    private EntrepriseRepository entrepriseRepository;

    @InjectMocks
    private EntrepriseServiceImpl service;

    @Test
    void should_create_entreprise_with_proprietaire_and_mark_trial_used() {
        EntrepriseRequest request = new EntrepriseRequest(
                "ACME", "ACME SARL", "NINEA-123", "RCCM-456", "Dakar"
        );
        Proprietaire proprietaire = new Proprietaire();
        when(entrepriseRepository.save(any(Entreprise.class))).thenAnswer(inv -> inv.getArgument(0));

        Entreprise result = service.create(request, proprietaire);

        ArgumentCaptor<Entreprise> captor = ArgumentCaptor.forClass(Entreprise.class);
        verify(entrepriseRepository).save(captor.capture());
        Entreprise saved = captor.getValue();
        assertThat(saved.getProprietaire()).isSameAs(proprietaire);
        assertThat(saved.getSigle()).isEqualTo("ACME");
        assertThat(saved.getRaisonSociale()).isEqualTo("ACME SARL");
        assertThat(saved.getNinea()).isEqualTo("NINEA-123");
        assertThat(saved.getRccm()).isEqualTo("RCCM-456");
        assertThat(saved.getAdresse()).isEqualTo("Dakar");
        assertThat(saved.isTrialUsed()).isTrue();
        assertThat(result).isSameAs(saved);
    }
}
