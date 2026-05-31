package org.store.stock.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.common.service.ValidatorService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.MouvementStockFilter;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.application.service.impl.MouvementStockServiceImpl;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.service.MouvementStockDomainService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MouvementStockServiceImplTest {

    @Mock private MouvementStockDomainService mouvementStockDomainService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private MouvementStockServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private UUID productId;
    private UUID stockId;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        productId = UUID.randomUUID();
        stockId = UUID.randomUUID();
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null, "owner", null, null, "OWNER", List.of("STOCK_READ"));
    }

    @Test
    void list_should_validate_filter_and_delegate_with_currentUser_entrepriseId() {
        MouvementStockFilter filter = new MouvementStockFilter(magasinId, productId, stockId, "ENTREE_ACHAT", null, null, null, null, 0, 10);
        Page<MouvementStockResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(mouvementStockDomainService.findResponsesByFilter(eq(filter), eq(entrepriseId))).thenReturn(page);

        Page<MouvementStockResponse> result = service.findAllByCurrentEntreprise(filter);

        verify(validatorService).validate(filter);
        verify(mouvementStockDomainService).findResponsesByFilter(eq(filter), eq(entrepriseId));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void filter_should_convert_dates_to_startOfDay_and_endOfDay() {
        MouvementStockFilter filter = new MouvementStockFilter(magasinId, null, null, null, "2026-05-01", "2026-05-14", null, null, 0, 10);

        assertThat(filter.fromDateTime()).isEqualTo(LocalDate.of(2026, 5, 1).atStartOfDay());
        assertThat(filter.toDateTime()).isEqualTo(LocalDate.of(2026, 5, 14).atTime(LocalTime.MAX));
    }

    @Test
    void filter_should_return_null_dateTime_when_blank_or_null_dates() {
        MouvementStockFilter filterNulls = new MouvementStockFilter(magasinId, null, null, null, null, null, null, null, 0, 10);
        MouvementStockFilter filterBlanks = new MouvementStockFilter(magasinId, null, null, null, "", "  ", null, null, 0, 10);

        assertThat(filterNulls.fromDateTime()).isNull();
        assertThat(filterNulls.toDateTime()).isNull();
        assertThat(filterBlanks.fromDateTime()).isNull();
        assertThat(filterBlanks.toDateTime()).isNull();
    }

    @Test
    void typeAsEnum_should_parse_valid_type_or_return_null() {
        MouvementStockFilter withType = new MouvementStockFilter(magasinId, null, null, "ENTREE_ACHAT", null, null, null, null, 0, 10);
        MouvementStockFilter withoutType = new MouvementStockFilter(magasinId, null, null, null, null, null, null, null, 0, 10);
        MouvementStockFilter blankType = new MouvementStockFilter(magasinId, null, null, "  ", null, null, null, null, 0, 10);

        assertThat(withType.typeAsEnum()).isEqualTo(MouvementStockType.ENTREE_ACHAT);
        assertThat(withoutType.typeAsEnum()).isNull();
        assertThat(blankType.typeAsEnum()).isNull();
    }
}
