package org.store.paiement.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.store.common.exceptions.BadArgumentException;
import org.store.country.domain.model.Country;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.repository.FacturationRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        FacturationRequest request = new FacturationRequest(moyenPaiement.getId(), pays.getId(), "77 000 00 00");

        Facturation result = service.create(request, moyenPaiement, pays);

        assertThat(result.getMoyenPaiement()).isEqualTo(moyenPaiement);
        assertThat(result.getPays()).isEqualTo(pays);
        assertThat(result.getNumeroFacturation()).isEqualTo("77 000 00 00");
        assertThat(result.isActif()).isTrue();
    }

    @Test
    void create_should_build_and_save_global_facturation_when_pays_is_null() {
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(UUID.randomUUID());
        FacturationRequest request = new FacturationRequest(moyenPaiement.getId(), null, "CARD-GLOBAL-001");

        Facturation result = service.create(request, moyenPaiement, null);

        assertThat(result.getPays()).isNull();
    }

    @Test
    void ensureUniqueMoyenPaysPair_should_pass_when_no_conflict() {
        UUID moyenPaiementId = UUID.randomUUID();
        UUID paysId = UUID.randomUUID();
        when(repository.existsByMoyenAndPays(moyenPaiementId, paysId, null)).thenReturn(false);

        service.ensureUniqueMoyenPaysPair(moyenPaiementId, paysId, null);
    }

    @Test
    void ensureUniqueMoyenPaysPair_should_throw_when_conflict_exists() {
        UUID moyenPaiementId = UUID.randomUUID();
        UUID paysId = UUID.randomUUID();
        when(repository.existsByMoyenAndPays(moyenPaiementId, paysId, null)).thenReturn(true);

        assertThatThrownBy(() -> service.ensureUniqueMoyenPaysPair(moyenPaiementId, paysId, null))
                .isInstanceOf(BadArgumentException.class);
    }
}
