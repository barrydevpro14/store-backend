package org.store.depense.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.depense.application.dto.CategoryDepenseRequest;
import org.store.depense.application.dto.CategoryDepenseResponse;
import org.store.depense.application.service.impl.CategoryDepenseServiceImpl;
import org.store.depense.domain.model.CategoryDepense;
import org.store.depense.domain.service.CategoryDepenseDomainService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryDepenseServiceImplTest {

    @Mock private CategoryDepenseDomainService categoryDepenseDomainService;
    @Mock private IEntrepriseService entrepriseService;
    @Mock private ICurrentUserService currentUserService;

    @InjectMocks
    private CategoryDepenseServiceImpl service;

    private UUID entrepriseId;
    private UUID categoryId;
    private Entreprise entreprise;
    private CategoryDepense category;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        category = new CategoryDepense();
        category.setId(categoryId);
        category.setNom("Loyer");
        category.setDescription("Loyer mensuel");
        category.setActif(true);
        category.setEntreprise(entreprise);
    }

    private UserPrincipal user() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null, "owner", null, null, "OWNER", List.of("EXPENSE_CREATE"));
    }

    @Test
    void create_should_persist_when_nom_available() {
        CategoryDepenseRequest req = new CategoryDepenseRequest("Loyer", "desc", null);

        when(currentUserService.getCurrent()).thenReturn(user());
        when(categoryDepenseDomainService.existsByNomAndEntrepriseId("Loyer", entrepriseId)).thenReturn(false);
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(categoryDepenseDomainService.create(eq(req), eq(entreprise))).thenReturn(category);

        CategoryDepenseResponse response = service.create(req);

        assertThat(response.nom()).isEqualTo("Loyer");
    }

    @Test
    void create_should_throw_when_nom_already_exists() {
        CategoryDepenseRequest req = new CategoryDepenseRequest("Loyer", null, null);

        when(currentUserService.getCurrent()).thenReturn(user());
        when(categoryDepenseDomainService.existsByNomAndEntrepriseId("Loyer", entrepriseId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(UniqueResourceException.class);

        verify(categoryDepenseDomainService, never()).create(any(), any());
    }

    @Test
    void findResponseById_should_return_when_owned() {
        when(categoryDepenseDomainService.findById(categoryId)).thenReturn(category);
        when(currentUserService.getCurrent()).thenReturn(user());

        CategoryDepenseResponse response = service.findResponseById(categoryId);

        assertThat(response.id()).isEqualTo(categoryId);
    }

    @Test
    void findResponseById_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        category.setEntreprise(other);

        when(categoryDepenseDomainService.findById(categoryId)).thenReturn(category);
        when(currentUserService.getCurrent()).thenReturn(user());

        assertThatThrownBy(() -> service.findResponseById(categoryId))
                .isInstanceOf(ForbiddenException.class);
    }
}
