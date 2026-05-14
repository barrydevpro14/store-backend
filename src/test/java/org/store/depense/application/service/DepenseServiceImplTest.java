package org.store.depense.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.depense.application.dto.DepenseFilter;
import org.store.depense.application.dto.DepenseRequest;
import org.store.depense.application.dto.DepenseResponse;
import org.store.depense.application.dto.DepenseTotalResponse;
import org.store.depense.application.service.impl.DepenseServiceImpl;
import org.store.depense.domain.model.CategoryDepense;
import org.store.depense.domain.model.Depense;
import org.store.depense.domain.service.DepenseDomainService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepenseServiceImplTest {

    @Mock private DepenseDomainService depenseDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private ICategoryDepenseService categoryDepenseService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private DepenseServiceImpl service;

    private UUID magasinId;
    private UUID categoryId;
    private UUID entrepriseId;
    private Entreprise entreprise;
    private Magasin magasin;
    private CategoryDepense category;
    private Depense depense;

    @BeforeEach
    void setUp() {
        magasinId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setNom("Magasin Central");
        magasin.setEntreprise(entreprise);

        category = new CategoryDepense();
        category.setId(categoryId);
        category.setNom("Loyer");
        category.setEntreprise(entreprise);

        depense = new Depense();
        depense.setId(UUID.randomUUID());
        depense.setMagasin(magasin);
        depense.setCategory(category);
        depense.setLibelle("Loyer mai");
        depense.setDateDepense(LocalDate.of(2026, 5, 1));
        depense.setMontant(new BigDecimal("250000.00"));
        depense.setModePaiement(MoyenPaiement.CASH);
    }

    private UserPrincipal user() {
        return new UserPrincipal(UUID.randomUUID(), entrepriseId, null, "owner", "PROPRIETAIRE", List.of("EXPENSE_CREATE"));
    }

    @Test
    void create_should_persist_when_scoping_ok() {
        DepenseRequest req = new DepenseRequest(magasinId, categoryId, "Loyer mai", "desc",
                LocalDate.of(2026, 5, 1), new BigDecimal("250000.00"), MoyenPaiement.CASH);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(categoryDepenseService.findById(categoryId)).thenReturn(category);
        when(categoryDepenseService.ensureBelongsToCurrentEntreprise(category)).thenReturn(category);
        when(depenseDomainService.create(eq(req), eq(magasin), eq(category))).thenReturn(depense);

        DepenseResponse response = service.create(req);

        assertThat(response.libelle()).isEqualTo("Loyer mai");
        assertThat(response.montant()).isEqualByComparingTo("250000.00");
        assertThat(response.modePaiement()).isEqualTo(MoyenPaiement.CASH);
    }

    @Test
    void create_should_propagate_forbidden_when_magasin_not_accessible() {
        DepenseRequest req = new DepenseRequest(magasinId, categoryId, "Loyer", null,
                LocalDate.now(), new BigDecimal("100.00"), MoyenPaiement.CASH);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void list_should_validate_and_delegate() {
        DepenseFilter filter = new DepenseFilter(magasinId, null, null, null, null, 0, 10);
        Page<DepenseResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(currentUserService.getCurrent()).thenReturn(user());
        when(depenseDomainService.findResponsesByFilter(eq(filter), eq(entrepriseId))).thenReturn(page);

        service.findAllByCurrentEntreprise(filter);

        verify(validatorService).validate(filter);
        verify(depenseDomainService).findResponsesByFilter(eq(filter), eq(entrepriseId));
    }

    @Test
    void computeTotal_should_validate_and_delegate() {
        DepenseFilter filter = new DepenseFilter(magasinId, null, null, "2026-05-01", "2026-05-31", 0, 1);
        DepenseTotalResponse expected = new DepenseTotalResponse(magasinId, new BigDecimal("750000.00"), 3L);

        when(currentUserService.getCurrent()).thenReturn(user());
        when(depenseDomainService.computeTotal(eq(filter), eq(entrepriseId))).thenReturn(expected);

        DepenseTotalResponse response = service.computeTotal(filter);

        verify(validatorService).validate(filter);
        assertThat(response.montantTotal()).isEqualByComparingTo("750000.00");
        assertThat(response.nombreDepenses()).isEqualTo(3L);
    }
}
