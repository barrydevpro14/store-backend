package org.store.abonnement.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.dto.RevenuPeriodFilter;
import org.store.abonnement.application.dto.RevenuRecordCommand;
import org.store.abonnement.application.service.impl.RevenuServiceImpl;
import org.store.abonnement.domain.model.Revenu;
import org.store.abonnement.domain.service.RevenuDomainService;
import org.store.country.domain.model.Country;
import org.store.entreprise.domain.model.Entreprise;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenuServiceImplTest {

    @Mock private RevenuDomainService revenuDomainService;
    @InjectMocks private RevenuServiceImpl service;

    @Test
    void record_should_save_a_revenu_row_built_from_the_entreprise_directly() {
        UUID entrepriseId = UUID.randomUUID();
        UUID countryId = UUID.randomUUID();
        Country country = new Country();
        country.setId(countryId);
        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        entreprise.setCountry(country);

        service.record(new RevenuRecordCommand(entreprise, LocalDate.of(2026, 8, 15), new BigDecimal("15000.00")));

        ArgumentCaptor<Revenu> captor = ArgumentCaptor.forClass(Revenu.class);
        verify(revenuDomainService).save(captor.capture());
        assertThat(captor.getValue().getEntreprise()).isEqualTo(entreprise);
        assertThat(captor.getValue().getCountry()).isEqualTo(country);
        assertThat(captor.getValue().getMontant()).isEqualByComparingTo("15000.00");
        assertThat(captor.getValue().getDatePaiement()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void getTotalForPeriod_should_delegate_straight_through_to_the_domain_service() {
        RevenuPeriodFilter filter = new RevenuPeriodFilter("2026-08-01", "2026-08-31", null, UUID.randomUUID());
        when(revenuDomainService.sumByPeriod(filter)).thenReturn(new BigDecimal("300000.00"));

        BigDecimal total = service.getTotalForPeriod(filter);

        assertThat(total).isEqualByComparingTo("300000.00");
        verify(revenuDomainService).sumByPeriod(filter);
    }
}
