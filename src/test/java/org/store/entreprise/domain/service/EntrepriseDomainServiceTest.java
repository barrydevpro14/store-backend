package org.store.entreprise.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.repository.EntrepriseRepository;
import org.store.users.domain.model.Proprietaire;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntrepriseDomainServiceTest {

    @Mock
    private EntrepriseRepository repository;

    @Mock
    private CountryDomainService countryDomainService;

    @InjectMocks
    private EntrepriseDomainService service;

    @Test
    void create_should_construct_entreprise_with_proprietaire_trial_used_and_active() {
        UUID countryId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Country country = new Country();
        EntrepriseRequest request = new EntrepriseRequest(
                "ACME", "ACME SARL", "NINEA-123", "RCCM-456", "Dakar", countryId, null);
        Proprietaire proprietaire = new Proprietaire();
        when(countryDomainService.findById(countryId)).thenReturn(country);
        when(repository.save(any(Entreprise.class))).thenAnswer(inv -> inv.getArgument(0));

        Entreprise result = service.create(request, proprietaire);

        ArgumentCaptor<Entreprise> captor = ArgumentCaptor.forClass(Entreprise.class);
        verify(repository).save(captor.capture());
        Entreprise saved = captor.getValue();
        assertThat(saved.getProprietaire()).isSameAs(proprietaire);
        assertThat(saved.getSigle()).isEqualTo("ACME");
        assertThat(saved.getRaisonSociale()).isEqualTo("ACME SARL");
        assertThat(saved.getNinea()).isEqualTo("NINEA-123");
        assertThat(saved.getRccm()).isEqualTo("RCCM-456");
        assertThat(saved.getAdresse()).isEqualTo("Dakar");
        assertThat(saved.getCountry()).isSameAs(country);
        assertThat(saved.isTrialUsed()).isTrue();
        assertThat(saved.isActif()).isTrue();
        assertThat(result).isSameAs(saved);
    }
}
