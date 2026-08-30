package org.store.paiement.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.store.common.dto.DataSelect;
import org.store.paiement.application.dto.MoyenPaiementSelectFilter;
import org.store.paiement.domain.repository.MoyenPaiementRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MoyenPaiementDomainServiceTest {

    private MoyenPaiementRepository repository;
    private MoyenPaiementDomainService service;

    @BeforeEach
    void setUp() {
        repository = mock(MoyenPaiementRepository.class);
        service = new MoyenPaiementDomainService(repository);
    }

    @Test
    void findSelectItems_should_build_like_pattern_and_delegate_to_repository() {
        UUID countryId = UUID.randomUUID();
        String searchTerm = "wave";
        int page = 0;
        int size = 10;

        when(repository.findSelectItems(any(UUID.class), any(String.class), any(String.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

        MoyenPaiementSelectFilter filter = new MoyenPaiementSelectFilter(countryId, searchTerm, page, size);
        service.findSelectItems(filter);

        ArgumentCaptor<UUID> countryIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> searchTermCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> searchPatternCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PageRequest> pageableCaptor = ArgumentCaptor.forClass(PageRequest.class);

        verify(repository).findSelectItems(countryIdCaptor.capture(), searchTermCaptor.capture(),
                                          searchPatternCaptor.capture(), pageableCaptor.capture());

        assertThat(countryIdCaptor.getValue()).isEqualTo(countryId);
        assertThat(searchTermCaptor.getValue()).isEqualTo(searchTerm);

        String pattern = searchPatternCaptor.getValue();
        assertThat(pattern).contains(searchTerm.toLowerCase());

        PageRequest pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(page);
        assertThat(pageable.getPageSize()).isEqualTo(size);
    }
}
