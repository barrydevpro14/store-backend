package org.store.paiement.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.tools.DateHelper;
import org.store.country.domain.model.Country;
import org.store.paiement.application.dto.FacturationFilter;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.repository.FacturationRepository;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FacturationDomainServiceTest {

    private FacturationRepository repository;
    private FacturationDomainService service;

    @BeforeEach
    void setUp() {
        repository = mock(FacturationRepository.class);
        service = new FacturationDomainService(repository);
        when(repository.save(any(Facturation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_should_build_and_save_facturation_with_pays() {
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(UUID.randomUUID());
        Country pays = new Country();
        pays.setId(UUID.randomUUID());
        FacturationRequest request = new FacturationRequest(moyenPaiement.getId(), Set.of(pays.getId()), "77 000 00 00");

        Facturation result = service.create(request, moyenPaiement, Set.of(pays));

        assertThat(result.getMoyenPaiement()).isEqualTo(moyenPaiement);
        assertThat(result.getPays()).containsExactly(pays);
        assertThat(result.getNumeroFacturation()).isEqualTo("77 000 00 00");
        assertThat(result.isActif()).isTrue();
    }

    @Test
    void create_should_build_and_save_global_facturation_when_pays_is_empty() {
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(UUID.randomUUID());
        FacturationRequest request = new FacturationRequest(moyenPaiement.getId(), Set.of(), "CARD-GLOBAL-001");

        Facturation result = service.create(request, moyenPaiement, Set.of());

        assertThat(result.getPays()).isEmpty();
    }

    @Test
    void ensureNoCountryOverlap_should_pass_when_no_conflict() {
        UUID moyenPaiementId = UUID.randomUUID();
        Set<UUID> paysIds = Set.of(UUID.randomUUID());
        when(repository.existsWithOverlappingCountry(moyenPaiementId, paysIds, null)).thenReturn(false);

        service.ensureNoCountryOverlap(moyenPaiementId, paysIds, null);
    }

    @Test
    void ensureNoCountryOverlap_should_throw_when_a_country_overlaps() {
        UUID moyenPaiementId = UUID.randomUUID();
        Set<UUID> paysIds = Set.of(UUID.randomUUID());
        when(repository.existsWithOverlappingCountry(moyenPaiementId, paysIds, null)).thenReturn(true);

        assertThatThrownBy(() -> service.ensureNoCountryOverlap(moyenPaiementId, paysIds, null))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void ensureNoCountryOverlap_should_check_global_uniqueness_when_paysIds_is_empty() {
        UUID moyenPaiementId = UUID.randomUUID();
        when(repository.existsGlobal(moyenPaiementId, null)).thenReturn(false);

        service.ensureNoCountryOverlap(moyenPaiementId, Set.of(), null);

        verify(repository).existsGlobal(moyenPaiementId, null);
    }

    @Test
    void ensureNoCountryOverlap_should_throw_when_another_global_facturation_already_exists() {
        UUID moyenPaiementId = UUID.randomUUID();
        when(repository.existsGlobal(moyenPaiementId, null)).thenReturn(true);

        assertThatThrownBy(() -> service.ensureNoCountryOverlap(moyenPaiementId, null, null))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void findResponsesByFilter_should_never_pass_a_null_date_to_the_repository() {
        FacturationFilter filterWithNoDateRange = new FacturationFilter(null, null, null, null, null, null, 0, 10);
        when(repository.findResponsesByFilter(isNull(), isNull(), isNull(), isNull(),
                eq(DateHelper.SENTINEL_START), eq(DateHelper.SENTINEL_END), any()))
                .thenReturn(Page.empty());

        service.findResponsesByFilter(filterWithNoDateRange);

        verify(repository).findResponsesByFilter(isNull(), isNull(), isNull(), isNull(),
                eq(DateHelper.SENTINEL_START), eq(DateHelper.SENTINEL_END), any());
    }

    @Test
    void findResponsesByFilter_should_build_a_like_pattern_for_numeroFacturation() {
        FacturationFilter filter = new FacturationFilter(null, null, "77 000", null, null, null, 0, 10);
        when(repository.findResponsesByFilter(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        service.findResponsesByFilter(filter);

        verify(repository).findResponsesByFilter(isNull(), isNull(), eq("%77 000%"), isNull(),
                eq(DateHelper.SENTINEL_START), eq(DateHelper.SENTINEL_END), any());
    }
}
