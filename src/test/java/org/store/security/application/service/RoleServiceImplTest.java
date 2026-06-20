package org.store.security.application.service;

import org.store.security.application.dto.RoleRequest;
import org.store.security.application.dto.RoleResponse;
import org.store.security.application.service.ICurrentUserService;
import org.store.security.application.service.impl.RoleServiceImpl;
import org.store.entreprise.domain.service.EntrepriseDomainService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.PermissionsDomainService;
import org.store.security.domain.service.RoleDomainService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock private RoleDomainService roleDomainService;
    @Mock private PermissionsDomainService permissionsDomainService;
    @Mock private EntrepriseDomainService entrepriseDomainService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private RoleServiceImpl service;

    // --- findByLibelle ---

    @Test
    void should_return_role_when_libelle_exists() {
        Role role = new Role();
        when(roleDomainService.findByLibelle("OWNER")).thenReturn(Optional.of(role));

        Role result = service.findByLibelle("OWNER");

        assertThat(result).isSameAs(role);
    }

    @Test
    void should_throw_entity_exception_when_libelle_unknown() {
        when(roleDomainService.findByLibelle("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByLibelle("GHOST"))
                .isInstanceOf(EntityException.class);
    }

    // --- findByIdWithPermissions ---

    @Test
    void findByIdWithPermissions_should_return_role_response_when_found() {
        UUID id = UUID.randomUUID();
        Role role = buildRole("SELLER", "Vendeur de magasin", new LinkedHashSet<>());
        RoleResponse expected = new RoleResponse(role);
        when(roleDomainService.findByIdWithPermissions(id)).thenReturn(Optional.of(expected));

        RoleResponse result = service.findByIdWithPermissions(id);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void findByIdWithPermissions_should_throw_entity_exception_when_not_found() {
        UUID id = UUID.randomUUID();
        when(roleDomainService.findByIdWithPermissions(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByIdWithPermissions(id))
                .isInstanceOf(EntityException.class);
    }

    // --- createSystemRole (permission resolution via public API) ---

    @Test
    void createSystemRole_should_set_resolved_permissions_only() {
        Permissions saleRead = buildPermission("SALE_READ");
        Permissions stockRead = buildPermission("STOCK_READ");
        Role created = buildRole("NEW_ROLE", null, new LinkedHashSet<>());
        created.setSysteme(true);

        when(roleDomainService.findByLibelle("NEW_ROLE")).thenReturn(Optional.empty());
        when(roleDomainService.create("NEW_ROLE", null, true)).thenReturn(created);
        when(permissionsDomainService.findByCode("SALE_READ")).thenReturn(Optional.of(saleRead));
        when(permissionsDomainService.findByCode("STOCK_READ")).thenReturn(Optional.of(stockRead));
        when(roleDomainService.setPermissions(eq(created), any())).thenReturn(created);

        service.createSystemRole(new RoleRequest("NEW_ROLE", null, List.of("SALE_READ", "STOCK_READ")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Permissions>> captor = ArgumentCaptor.forClass(Set.class);
        verify(roleDomainService).setPermissions(eq(created), captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(saleRead, stockRead);
    }

    @Test
    void createSystemRole_should_ignore_unknown_permission_codes() {
        Permissions saleRead = buildPermission("SALE_READ");
        Role created = buildRole("NEW_ROLE", null, new LinkedHashSet<>());
        created.setSysteme(true);

        when(roleDomainService.findByLibelle("NEW_ROLE")).thenReturn(Optional.empty());
        when(roleDomainService.create("NEW_ROLE", null, true)).thenReturn(created);
        when(permissionsDomainService.findByCode("SALE_READ")).thenReturn(Optional.of(saleRead));
        when(permissionsDomainService.findByCode("UNKNOWN_CODE")).thenReturn(Optional.empty());
        when(roleDomainService.setPermissions(eq(created), any())).thenReturn(created);

        service.createSystemRole(new RoleRequest("NEW_ROLE", null, List.of("SALE_READ", "UNKNOWN_CODE")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Permissions>> captor = ArgumentCaptor.forClass(Set.class);
        verify(roleDomainService).setPermissions(eq(created), captor.capture());
        assertThat(captor.getValue()).containsExactly(saleRead);
    }

    @Test
    void createSystemRole_should_not_set_permissions_when_list_is_empty() {
        Role created = buildRole("NEW_ROLE", null, new LinkedHashSet<>());
        created.setSysteme(true);

        when(roleDomainService.findByLibelle("NEW_ROLE")).thenReturn(Optional.empty());
        when(roleDomainService.create("NEW_ROLE", null, true)).thenReturn(created);

        service.createSystemRole(new RoleRequest("NEW_ROLE", null, List.of()));

        verify(roleDomainService, org.mockito.Mockito.never()).setPermissions(any(), any());
    }

    // --- helpers ---

    private static Permissions buildPermission(String code) {
        Permissions permission = new Permissions();
        permission.setId(UUID.randomUUID());
        permission.setCode(code);
        return permission;
    }

    private static Role buildRole(String libelle, String description, Set<Permissions> permissions) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setLibelle(libelle);
        role.setDescription(description);
        role.setPermissions(new LinkedHashSet<>(permissions));
        return role;
    }
}
