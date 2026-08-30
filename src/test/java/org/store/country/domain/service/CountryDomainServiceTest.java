package org.store.country.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.store.common.exceptions.EntityException;
import org.store.country.domain.model.Country;
import org.store.country.domain.repository.CountryRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CountryDomainServiceTest {

    private CountryRepository repository;
    private CountryDomainService service;

    @BeforeEach
    void setUp() {
        repository = mock(CountryRepository.class);
        service = new CountryDomainService(repository);
    }

    @Test
    void findAllByIds_should_return_empty_list_when_ids_is_null() {
        assertThat(service.findAllByIds(null)).isEmpty();
    }

    @Test
    void findAllByIds_should_return_all_found_countries() {
        UUID senegalId = UUID.randomUUID();
        Country senegal = new Country();
        senegal.setId(senegalId);
        when(repository.findAllByIdIn(Set.of(senegalId))).thenReturn(List.of(senegal));

        List<Country> result = service.findAllByIds(Set.of(senegalId));

        assertThat(result).containsExactly(senegal);
    }

    @Test
    void findAllByIds_should_throw_when_a_requested_id_does_not_resolve() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findAllByIdIn(Set.of(unknownId))).thenReturn(List.of());

        assertThatThrownBy(() -> service.findAllByIds(Set.of(unknownId)))
                .isInstanceOf(EntityException.class);
    }
}
