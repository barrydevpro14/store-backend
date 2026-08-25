package org.store.plateforme.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
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
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepensePlateformeServiceImplTest {

    @Mock private DepensePlateformeDomainService domainService;
    @Mock private ICategoryDepensePlateformeService categoryService;
    @Mock private IMoyenPaiementService moyenPaiementService;
    @Mock private CountryDomainService countryDomainService;
    @Mock private ValidatorService validatorService;
    @Mock private IAuditEventPublisher auditEventPublisher;
    @Mock private ICurrentUserService currentUserService;
    @InjectMocks private DepensePlateformeServiceImpl service;

    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID MOYEN_ID = UUID.randomUUID();
    private static final UUID COUNTRY_ID = UUID.randomUUID();

    private static UserPrincipal callerFixture() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), null, null, "admin", null, null, "ADMIN", List.of());
    }

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
                new BigDecimal("500000.00"), MOYEN_ID, COUNTRY_ID, null);

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
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        DepensePlateformeResponse response = service.create(request);

        assertThat(response.libelle()).isEqualTo("Serveur AWS");
        assertThat(response.country()).isNotNull();
        assertThat(response.country().countryCode()).isEqualTo("SN");
    }

    @Test
    void create_should_pass_null_country_when_countryId_absent() {
        DepensePlateformeRequest request = new DepensePlateformeRequest(
                CATEGORY_ID, "Outil SaaS global", null, LocalDate.of(2026, 8, 1),
                new BigDecimal("50000.00"), MOYEN_ID, null, null);

        DepensePlateforme saved = new DepensePlateforme();
        saved.setId(UUID.randomUUID());
        saved.setCategory(category());
        saved.setLibelle("Outil SaaS global");
        saved.setMontant(new BigDecimal("50000.00"));
        saved.setModePaiement(moyen());

        when(categoryService.findById(CATEGORY_ID)).thenReturn(category());
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyen());
        when(domainService.create(eq(request), any(), any(), eq(null))).thenReturn(saved);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        DepensePlateformeResponse response = service.create(request);

        assertThat(response.country()).isNull();
    }

    @Test
    void create_should_publish_audit_event_on_success() {
        DepensePlateformeRequest request = new DepensePlateformeRequest(
                CATEGORY_ID, "Serveur AWS", null, LocalDate.of(2026, 8, 1),
                new BigDecimal("500000.00"), MOYEN_ID, null, null);

        DepensePlateforme saved = new DepensePlateforme();
        saved.setId(UUID.randomUUID());
        saved.setCategory(category());
        saved.setLibelle("Serveur AWS");
        saved.setMontant(new BigDecimal("500000.00"));
        saved.setModePaiement(moyen());

        when(categoryService.findById(CATEGORY_ID)).thenReturn(category());
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyen());
        when(domainService.create(eq(request), any(), any(), eq(null))).thenReturn(saved);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        service.create(request);

        verify(auditEventPublisher, times(1)).publish(argThat(event ->
                event.action() == AuditAction.DEPENSE_PLATEFORME_CREATED
                        && event.entityType() == AuditEntityType.DEPENSE_PLATEFORME
                        && event.entrepriseId() == null
                        && event.magasinId() == null));
    }

    @Test
    void create_with_actif_false_should_produce_inactive_depense() {
        DepensePlateformeRequest request = new DepensePlateformeRequest(
                CATEGORY_ID, "Serveur AWS", null, LocalDate.of(2026, 8, 1),
                new BigDecimal("500000.00"), MOYEN_ID, null, false);

        DepensePlateforme saved = new DepensePlateforme();
        saved.setId(UUID.randomUUID());
        saved.setCategory(category());
        saved.setLibelle("Serveur AWS");
        saved.setMontant(new BigDecimal("500000.00"));
        saved.setModePaiement(moyen());
        saved.setActif(false);

        when(categoryService.findById(CATEGORY_ID)).thenReturn(category());
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyen());
        when(domainService.create(eq(request), any(), any(), eq(null))).thenReturn(saved);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        DepensePlateformeResponse response = service.create(request);

        assertThat(response.actif()).isFalse();
    }

    @Test
    void update_should_publish_audit_event_on_success() {
        UUID id = UUID.randomUUID();
        DepensePlateformeRequest request = new DepensePlateformeRequest(
                CATEGORY_ID, "Serveur AWS renommé", null, LocalDate.of(2026, 8, 1),
                new BigDecimal("600000.00"), MOYEN_ID, null, null);

        DepensePlateforme existing = new DepensePlateforme();
        existing.setId(id);
        existing.setCategory(category());
        existing.setLibelle("Serveur AWS");
        existing.setMontant(new BigDecimal("500000.00"));
        existing.setModePaiement(moyen());

        when(domainService.findById(id)).thenReturn(existing);
        when(categoryService.findById(CATEGORY_ID)).thenReturn(category());
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyen());
        when(domainService.save(existing)).thenReturn(existing);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        DepensePlateformeResponse response = service.update(id, request);

        assertThat(response.libelle()).isEqualTo("Serveur AWS renommé");
        verify(auditEventPublisher, times(1)).publish(argThat(event ->
                event.action() == AuditAction.DEPENSE_PLATEFORME_UPDATED
                        && event.entityType() == AuditEntityType.DEPENSE_PLATEFORME
                        && event.entrepriseId() == null
                        && event.magasinId() == null));
    }

    @Test
    void update_with_actif_true_should_reactivate_previously_deactivated_depense() {
        UUID id = UUID.randomUUID();
        DepensePlateformeRequest request = new DepensePlateformeRequest(
                CATEGORY_ID, "Serveur AWS", null, LocalDate.of(2026, 8, 1),
                new BigDecimal("500000.00"), MOYEN_ID, null, true);

        DepensePlateforme existing = new DepensePlateforme();
        existing.setId(id);
        existing.setCategory(category());
        existing.setLibelle("Serveur AWS");
        existing.setMontant(new BigDecimal("500000.00"));
        existing.setModePaiement(moyen());
        existing.setActif(false);

        when(domainService.findById(id)).thenReturn(existing);
        when(categoryService.findById(CATEGORY_ID)).thenReturn(category());
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyen());
        when(domainService.save(existing)).thenReturn(existing);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        DepensePlateformeResponse response = service.update(id, request);

        assertThat(response.actif()).isTrue();
        verify(domainService).save(argThat(DepensePlateforme::isActif));
    }

    @Test
    void update_with_actif_null_should_leave_existing_actif_value_untouched() {
        UUID id = UUID.randomUUID();
        DepensePlateformeRequest request = new DepensePlateformeRequest(
                CATEGORY_ID, "Serveur AWS renommé", null, LocalDate.of(2026, 8, 1),
                new BigDecimal("600000.00"), MOYEN_ID, null, null);

        DepensePlateforme existing = new DepensePlateforme();
        existing.setId(id);
        existing.setCategory(category());
        existing.setLibelle("Serveur AWS");
        existing.setMontant(new BigDecimal("500000.00"));
        existing.setModePaiement(moyen());
        existing.setActif(false);

        when(domainService.findById(id)).thenReturn(existing);
        when(categoryService.findById(CATEGORY_ID)).thenReturn(category());
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyen());
        when(domainService.save(existing)).thenReturn(existing);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        DepensePlateformeResponse response = service.update(id, request);

        assertThat(response.actif()).isFalse();
        verify(domainService).save(argThat(d -> !d.isActif()));
    }

    @Test
    void delete_should_deactivate_instead_of_hard_delete() {
        UUID id = UUID.randomUUID();
        DepensePlateforme existing = new DepensePlateforme();
        existing.setId(id);
        existing.setLibelle("Serveur AWS");
        existing.setActif(true);

        when(domainService.findById(id)).thenReturn(existing);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        service.delete(id);

        verify(domainService).save(argThat(d -> !d.isActif()));
        verify(domainService, never()).delete(any());
    }

    @Test
    void delete_should_publish_audit_event() {
        UUID id = UUID.randomUUID();
        DepensePlateforme existing = new DepensePlateforme();
        existing.setId(id);
        existing.setLibelle("Serveur AWS");
        existing.setActif(true);

        when(domainService.findById(id)).thenReturn(existing);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        service.delete(id);

        verify(auditEventPublisher, times(1)).publish(argThat(event ->
                event.action() == AuditAction.DEPENSE_PLATEFORME_DELETED
                        && event.entityType() == AuditEntityType.DEPENSE_PLATEFORME
                        && event.entrepriseId() == null
                        && event.magasinId() == null));
    }

    @Test
    void findAll_should_pass_actif_filter_through() {
        DepensePlateformeFilter filter = new DepensePlateformeFilter(null, null, null, false, null, null, null, 0, 10);
        when(domainService.findResponsesByFilter(filter)).thenReturn(org.springframework.data.domain.Page.empty());

        service.findAll(filter);

        verify(domainService).findResponsesByFilter(argThat(f -> f.actif() == Boolean.FALSE));
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
        DepensePlateformeFilter filter = new DepensePlateformeFilter(null, null, null, null, null, null, null, 0, 10);
        when(domainService.findResponsesByFilter(filter)).thenReturn(org.springframework.data.domain.Page.empty());

        service.findAll(filter);

        verify(validatorService).validate(filter);
    }
}
