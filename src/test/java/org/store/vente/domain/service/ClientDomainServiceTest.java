package org.store.vente.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.common.tools.DateHelper;
import org.store.common.tools.LikePatternHelper;
import org.store.magasin.domain.model.Magasin;
import org.store.vente.application.dto.ClientFilter;
import org.store.vente.application.dto.ClientRequest;
import org.store.vente.application.dto.ClientResponse;
import org.store.vente.domain.model.Client;
import org.store.vente.domain.repository.ClientRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientDomainServiceTest {

    @Mock
    private ClientRepository repository;

    @InjectMocks
    private ClientDomainService service;

    private UUID magasinId;
    private UUID entrepriseId;
    private Magasin magasin;

    @BeforeEach
    void setUp() {
        magasinId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();
        magasin = new Magasin();
        magasin.setId(magasinId);
    }

    @Test
    void create_should_set_all_fields_and_return_saved_entity() {
        ClientRequest request = new ClientRequest(
                "Diallo", "Mamadou", "mamadou@example.com", "+221770000001", "Dakar", magasinId);

        when(repository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        Client result = service.create(request, magasin);

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(repository).save(captor.capture());
        Client saved = captor.getValue();

        assertThat(saved.getNom()).isEqualTo("Diallo");
        assertThat(saved.getPrenom()).isEqualTo("Mamadou");
        assertThat(saved.getEmail()).isEqualTo("mamadou@example.com");
        assertThat(saved.getTelephone()).isEqualTo("+221770000001");
        assertThat(saved.getAdresse()).isEqualTo("Dakar");
        assertThat(saved.getMagasin()).isSameAs(magasin);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void create_should_handle_nullable_optional_fields() {
        ClientRequest request = new ClientRequest("Diop", null, null, "+221770000002", null, magasinId);

        when(repository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        Client result = service.create(request, magasin);

        assertThat(result.getNom()).isEqualTo("Diop");
        assertThat(result.getPrenom()).isNull();
        assertThat(result.getEmail()).isNull();
        assertThat(result.getAdresse()).isNull();
    }

    @Test
    void findResponsesByMagasinId_should_delegate_to_repository_with_correct_args() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);
        ClientFilter filter = new ClientFilter("Diallo", "Mama", start, end, 0, 10);

        ClientResponse response = new ClientResponse(UUID.randomUUID(), "Diallo", "Mama", null, "+221770000001", null);
        Page<ClientResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        String expectedNomPattern = LikePatternHelper.toLikePattern("Diallo");
        String expectedPrenomPattern = LikePatternHelper.toLikePattern("Mama");
        LocalDateTime expectedStart = filter.createdStartDateTime();
        LocalDateTime expectedEnd = filter.createdEndDateTime();

        when(repository.findResponsesByMagasinId(
                eq(magasinId),
                eq(expectedNomPattern),
                eq(expectedPrenomPattern),
                eq(expectedStart),
                eq(expectedEnd),
                eq(filter.toPageable())))
                .thenReturn(page);

        Page<ClientResponse> result = service.findResponsesByMagasinId(magasinId, filter);

        assertThat(result.getContent()).containsExactly(response);
        verify(repository).findResponsesByMagasinId(
                magasinId, expectedNomPattern, expectedPrenomPattern,
                expectedStart, expectedEnd, filter.toPageable());
    }

    @Test
    void findResponsesByMagasinId_should_use_sentinel_when_dates_are_null() {
        ClientFilter filter = new ClientFilter(null, null, null, null, 0, 20);

        Page<ClientResponse> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(repository.findResponsesByMagasinId(
                eq(magasinId),
                eq(null),
                eq(null),
                eq(DateHelper.SENTINEL_START),
                eq(DateHelper.SENTINEL_END),
                any()))
                .thenReturn(empty);

        Page<ClientResponse> result = service.findResponsesByMagasinId(magasinId, filter);

        assertThat(result).isEmpty();
    }

    @Test
    void findResponsesByEntrepriseId_should_delegate_to_repository_with_correct_args() {
        LocalDate start = LocalDate.of(2025, 3, 1);
        LocalDate end = LocalDate.of(2025, 6, 30);
        ClientFilter filter = new ClientFilter("Ndiaye", null, start, end, 1, 5);

        String expectedNomPattern = LikePatternHelper.toLikePattern("Ndiaye");
        LocalDateTime expectedStart = filter.createdStartDateTime();
        LocalDateTime expectedEnd = filter.createdEndDateTime();

        Page<ClientResponse> page = new PageImpl<>(List.of(), PageRequest.of(1, 5), 0);

        when(repository.findResponsesByEntrepriseId(
                eq(entrepriseId),
                eq(expectedNomPattern),
                eq(null),
                eq(expectedStart),
                eq(expectedEnd),
                eq(filter.toPageable())))
                .thenReturn(page);

        Page<ClientResponse> result = service.findResponsesByEntrepriseId(entrepriseId, filter);

        assertThat(result).isEmpty();
        verify(repository).findResponsesByEntrepriseId(
                entrepriseId, expectedNomPattern, null,
                expectedStart, expectedEnd, filter.toPageable());
    }

    @Test
    void findResponsesByEntrepriseId_should_use_sentinel_when_dates_are_null() {
        ClientFilter filter = new ClientFilter(null, null, null, null, 0, 10);

        Page<ClientResponse> empty = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(repository.findResponsesByEntrepriseId(
                eq(entrepriseId),
                eq(null),
                eq(null),
                eq(DateHelper.SENTINEL_START),
                eq(DateHelper.SENTINEL_END),
                any()))
                .thenReturn(empty);

        Page<ClientResponse> result = service.findResponsesByEntrepriseId(entrepriseId, filter);

        assertThat(result).isEmpty();
    }
}
