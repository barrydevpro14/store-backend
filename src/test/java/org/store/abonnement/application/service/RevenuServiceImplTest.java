package org.store.abonnement.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.service.impl.RevenuServiceImpl;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Revenu;
import org.store.abonnement.domain.service.RevenuDomainService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenuServiceImplTest {

    @Mock private RevenuDomainService revenuDomainService;
    @Mock private IEntrepriseService entrepriseService;
    @Mock private CountryDomainService countryDomainService;
    @Mock private IAbonnementService abonnementService;
    @InjectMocks private RevenuServiceImpl service;

    @Test
    void record_should_resolve_entreprise_and_country_then_save() {
        UUID entrepriseId = UUID.randomUUID();
        UUID countryId = UUID.randomUUID();
        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        Country country = new Country();
        country.setId(countryId);

        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(countryDomainService.findById(countryId)).thenReturn(country);

        service.record(entrepriseId, countryId, LocalDate.of(2026, 8, 15), new BigDecimal("15000.00"));

        ArgumentCaptor<Revenu> captor = ArgumentCaptor.forClass(Revenu.class);
        verify(revenuDomainService).save(captor.capture());
        assertThat(captor.getValue().getEntreprise()).isEqualTo(entreprise);
        assertThat(captor.getValue().getCountry()).isEqualTo(country);
        assertThat(captor.getValue().getMontant()).isEqualByComparingTo("15000.00");
    }

    @Test
    void getTotalForPeriod_should_resolve_abonnementId_to_entrepriseId() {
        UUID abonnementId = UUID.randomUUID();
        UUID entrepriseId = UUID.randomUUID();
        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        Abonnement abonnement = new Abonnement();
        abonnement.setEntreprise(entreprise);

        when(abonnementService.findById(abonnementId)).thenReturn(abonnement);
        when(revenuDomainService.sumByPeriod("2026-08-01", "2026-08-31", null, entrepriseId))
                .thenReturn(new BigDecimal("300000.00"));

        BigDecimal total = service.getTotalForPeriod("2026-08-01", "2026-08-31", null, abonnementId);

        assertThat(total).isEqualByComparingTo("300000.00");
    }

    @Test
    void getTotalForPeriod_should_pass_null_entrepriseId_when_abonnementId_absent() {
        when(revenuDomainService.sumByPeriod("2026-08-01", "2026-08-31", null, null))
                .thenReturn(new BigDecimal("450000.00"));

        BigDecimal total = service.getTotalForPeriod("2026-08-01", "2026-08-31", null, null);

        assertThat(total).isEqualByComparingTo("450000.00");
        verifyNoInteractions(abonnementService);
    }
}
