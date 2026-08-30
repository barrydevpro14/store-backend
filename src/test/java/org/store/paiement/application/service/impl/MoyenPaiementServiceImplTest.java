package org.store.paiement.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.paiement.application.dto.MoyenPaiementRequest;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.service.MoyenPaiementDomainService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MoyenPaiementServiceImplTest {

    private MoyenPaiementDomainService domainService;
    private ValidatorService validatorService;
    private CountryDomainService countryDomainService;
    private MoyenPaiementServiceImpl service;

    @BeforeEach
    void setUp() {
        domainService = mock(MoyenPaiementDomainService.class);
        validatorService = mock(ValidatorService.class);
        countryDomainService = mock(CountryDomainService.class);
        service = new MoyenPaiementServiceImpl(domainService, validatorService, countryDomainService);
        when(domainService.save(any(MoyenPaiement.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_should_leave_pays_empty_when_paysIds_is_null() {
        when(domainService.findAll()).thenReturn(List.of());
        when(countryDomainService.findAllByIds(null)).thenReturn(List.of());

        MoyenPaiementResponse response = service.create(new MoyenPaiementRequest("Wave", null));

        assertThat(response.pays()).isEmpty();
    }

    @Test
    void create_should_attach_pays_when_paysIds_provided() {
        when(domainService.findAll()).thenReturn(List.of());
        UUID senegalId = UUID.randomUUID();
        Country senegal = new Country();
        senegal.setId(senegalId);
        senegal.setName("Sénégal");
        when(countryDomainService.findAllByIds(Set.of(senegalId))).thenReturn(List.of(senegal));

        MoyenPaiementResponse response = service.create(new MoyenPaiementRequest("Wave", Set.of(senegalId)));

        assertThat(response.pays()).extracting("id").containsExactly(senegalId);
    }

    @Test
    void create_should_throw_when_libelle_already_exists() {
        MoyenPaiement existing = new MoyenPaiement();
        existing.setId(UUID.randomUUID());
        existing.setLibelle("Wave");
        when(domainService.findAll()).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create(new MoyenPaiementRequest("Wave", null)))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void update_should_replace_pays_entirely() {
        UUID id = UUID.randomUUID();
        MoyenPaiement existing = new MoyenPaiement();
        existing.setId(id);
        existing.setLibelle("Wave");
        UUID guineeId = UUID.randomUUID();
        Country guinee = new Country();
        guinee.setId(guineeId);
        guinee.setName("Guinée");
        when(domainService.findById(id)).thenReturn(existing);
        when(domainService.findAll()).thenReturn(List.of(existing));
        when(countryDomainService.findAllByIds(Set.of(guineeId))).thenReturn(List.of(guinee));

        MoyenPaiementResponse response = service.update(id, new MoyenPaiementRequest("Wave", Set.of(guineeId)));

        assertThat(response.pays()).extracting("id").containsExactly(guineeId);
    }
}
