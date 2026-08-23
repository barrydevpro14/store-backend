package org.store.plateforme.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.plateforme.application.dto.DepensePlateformeFilter;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.service.impl.DepensePlateformeServiceImpl;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.model.DepensePlateforme;
import org.store.plateforme.domain.service.DepensePlateformeDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepensePlateformeServiceImplTest {

    @Mock private DepensePlateformeDomainService domainService;
    @Mock private ICategoryDepensePlateformeService categoryService;
    @Mock private IMoyenPaiementService moyenPaiementService;
    @Mock private CountryDomainService countryDomainService;
    @Mock private ValidatorService validatorService;
    @InjectMocks private DepensePlateformeServiceImpl service;

    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID MOYEN_ID = UUID.randomUUID();
    private static final UUID COUNTRY_ID = UUID.randomUUID();

    private CategoryDepensePlateforme category() {
        CategoryDepensePlateforme c = new CategoryDepensePlateforme();
        c.setId(CATEGORY_ID);
        c.setNom("Hébergement");
        return c;
    }

    private MoyenPaiement moyen() {
        MoyenPaiement m = new MoyenPaiement();
        m.setId(MOYEN_ID);
        m.setLibelle("Virement");
        return m;
    }

    private Country country() {
        Country c = new Country();
        c.setId(COUNTRY_ID);
        c.setName("Sénégal");
        c.setCountryCode("SN");
        c.setCurrency("XOF");
        return c;
    }

    @Test
    void create_should_resolve_country_when_countryId_present() {
        DepensePlateformeRequest request = new DepensePlateformeRequest(
                CATEGORY_ID, "Serveur AWS", null, LocalDate.of(2026, 8, 1),
                new BigDecimal("500000.00"), MOYEN_ID, COUNTRY_ID);

        DepensePlateforme saved = new DepensePlateforme();
        saved.setId(UUID.randomUUID());
        saved.setCategory(category());
        saved.setLibelle("Serveur AWS");
        saved.setMontant(new BigDecimal("500000.00"));
        saved.setModePaiement(moyen());
        saved.setCountry(country());

        when(categoryService.findById(CATEGORY_ID)).thenReturn(category());
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyen());
        when(countryDomainService.findById(COUNTRY_ID)).thenReturn(country());
        when(domainService.create(eq(request), any(), any(), any())).thenReturn(saved);

        DepensePlateformeResponse response = service.create(request);

        assertThat(response.libelle()).isEqualTo("Serveur AWS");
        assertThat(response.country()).isNotNull();
        assertThat(response.country().countryCode()).isEqualTo("SN");
    }

    @Test
    void create_should_pass_null_country_when_countryId_absent() {
        DepensePlateformeRequest request = new DepensePlateformeRequest(
                CATEGORY_ID, "Outil SaaS global", null, LocalDate.of(2026, 8, 1),
                new BigDecimal("50000.00"), MOYEN_ID, null);

        DepensePlateforme saved = new DepensePlateforme();
        saved.setId(UUID.randomUUID());
        saved.setCategory(category());
        saved.setLibelle("Outil SaaS global");
        saved.setMontant(new BigDecimal("50000.00"));
        saved.setModePaiement(moyen());

        when(categoryService.findById(CATEGORY_ID)).thenReturn(category());
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyen());
        when(domainService.create(eq(request), any(), any(), eq(null))).thenReturn(saved);

        DepensePlateformeResponse response = service.create(request);

        assertThat(response.country()).isNull();
    }

    @Test
    void computeTotal_with_period_and_country_should_delegate_to_domainService_sumByPeriod() {
        when(domainService.sumByPeriod("2026-08-01", "2026-08-31", COUNTRY_ID))
                .thenReturn(new BigDecimal("750000.00"));

        BigDecimal total = service.computeTotal("2026-08-01", "2026-08-31", COUNTRY_ID);

        assertThat(total).isEqualByComparingTo("750000.00");
        verify(domainService).sumByPeriod("2026-08-01", "2026-08-31", COUNTRY_ID);
    }

    @Test
    void findAll_should_validate_filter_before_delegating() {
        DepensePlateformeFilter filter = new DepensePlateformeFilter(null, null, null, null, null, null, 0, 10);
        when(domainService.findResponsesByFilter(filter)).thenReturn(org.springframework.data.domain.Page.empty());

        service.findAll(filter);

        verify(validatorService).validate(filter);
    }
}
