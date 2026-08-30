package org.store.paiement.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.service.FacturationDomainService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FacturationServiceImplTest {

    private FacturationDomainService domainService;
    private IMoyenPaiementService moyenPaiementService;
    private CountryDomainService countryDomainService;
    private ValidatorService validatorService;
    private FacturationServiceImpl service;

    @BeforeEach
    void setUp() {
        domainService = mock(FacturationDomainService.class);
        moyenPaiementService = mock(IMoyenPaiementService.class);
        countryDomainService = mock(CountryDomainService.class);
        validatorService = mock(ValidatorService.class);
        service = new FacturationServiceImpl(domainService, moyenPaiementService, countryDomainService, validatorService);
    }

    @Test
    void create_should_resolve_moyen_and_pays_then_delegate_to_domain_service() {
        UUID moyenPaiementId = UUID.randomUUID();
        UUID paysId = UUID.randomUUID();
        FacturationRequest request = new FacturationRequest(moyenPaiementId, paysId, "77 000 00 00");
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(moyenPaiementId);
        Country pays = new Country();
        pays.setId(paysId);
        Facturation created = new Facturation();
        created.setId(UUID.randomUUID());
        created.setMoyenPaiement(moyenPaiement);
        created.setPays(pays);
        created.setNumeroFacturation("77 000 00 00");
        when(moyenPaiementService.findById(moyenPaiementId)).thenReturn(moyenPaiement);
        when(countryDomainService.findById(paysId)).thenReturn(pays);
        when(domainService.create(request, moyenPaiement, pays)).thenReturn(created);

        FacturationResponse response = service.create(request);

        assertThat(response.numeroFacturation()).isEqualTo("77 000 00 00");
        verify(domainService).ensureUniqueMoyenPaysPair(moyenPaiementId, paysId, null);
    }

    @Test
    void create_should_not_resolve_pays_when_paysId_is_null() {
        UUID moyenPaiementId = UUID.randomUUID();
        FacturationRequest request = new FacturationRequest(moyenPaiementId, null, "CARD-GLOBAL");
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(moyenPaiementId);
        Facturation created = new Facturation();
        created.setId(UUID.randomUUID());
        created.setMoyenPaiement(moyenPaiement);
        created.setNumeroFacturation("CARD-GLOBAL");
        when(moyenPaiementService.findById(moyenPaiementId)).thenReturn(moyenPaiement);
        when(domainService.create(eq(request), eq(moyenPaiement), isNull())).thenReturn(created);

        FacturationResponse response = service.create(request);

        assertThat(response.pays()).isNull();
        verify(countryDomainService, org.mockito.Mockito.never()).findById(any());
    }

    @Test
    void create_should_throw_when_moyen_pays_pair_already_exists() {
        UUID moyenPaiementId = UUID.randomUUID();
        UUID paysId = UUID.randomUUID();
        FacturationRequest request = new FacturationRequest(moyenPaiementId, paysId, "77 000 00 00");
        org.mockito.Mockito.doThrow(new BadArgumentException("facturation.alreadyExists"))
                .when(domainService).ensureUniqueMoyenPaysPair(moyenPaiementId, paysId, null);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BadArgumentException.class);
    }

    @Test
    void update_should_exclude_current_id_from_uniqueness_check() {
        UUID id = UUID.randomUUID();
        UUID moyenPaiementId = UUID.randomUUID();
        FacturationRequest request = new FacturationRequest(moyenPaiementId, null, "NEW-NUMBER");
        Facturation existing = new Facturation();
        existing.setId(id);
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(moyenPaiementId);
        when(domainService.findById(id)).thenReturn(existing);
        when(moyenPaiementService.findById(moyenPaiementId)).thenReturn(moyenPaiement);
        when(domainService.save(existing)).thenReturn(existing);

        service.update(id, request);

        verify(domainService).ensureUniqueMoyenPaysPair(moyenPaiementId, null, id);
        assertThat(existing.getNumeroFacturation()).isEqualTo("NEW-NUMBER");
    }

    @Test
    void activate_should_set_actif_true() {
        UUID id = UUID.randomUUID();
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(UUID.randomUUID());
        Facturation facturation = new Facturation();
        facturation.setId(id);
        facturation.setMoyenPaiement(moyenPaiement);
        facturation.setActif(false);
        when(domainService.findById(id)).thenReturn(facturation);
        when(domainService.save(facturation)).thenReturn(facturation);

        service.activate(id);

        assertThat(facturation.isActif()).isTrue();
    }

    @Test
    void deactivate_should_set_actif_false() {
        UUID id = UUID.randomUUID();
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(UUID.randomUUID());
        Facturation facturation = new Facturation();
        facturation.setId(id);
        facturation.setMoyenPaiement(moyenPaiement);
        facturation.setActif(true);
        when(domainService.findById(id)).thenReturn(facturation);
        when(domainService.save(facturation)).thenReturn(facturation);

        service.deactivate(id);

        assertThat(facturation.isActif()).isFalse();
    }

    @Test
    void delete_should_delegate_to_domain_service() {
        UUID id = UUID.randomUUID();
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(UUID.randomUUID());
        Facturation facturation = new Facturation();
        facturation.setId(id);
        facturation.setMoyenPaiement(moyenPaiement);
        when(domainService.findById(id)).thenReturn(facturation);

        service.delete(id);

        verify(domainService).delete(facturation);
    }
}
