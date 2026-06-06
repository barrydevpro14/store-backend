package org.store.security.application.service;

import org.store.security.application.dto.RoleResponse;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.enums.PermissionCode;
import org.store.security.application.service.ICurrentUserService;
import org.store.security.application.service.impl.RoleServiceImpl;
import org.store.entreprise.domain.service.EntrepriseDomainService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    void findAllScoped_should_project_each_role_with_permissions_sorted_by_code_for_admin() {
        Permissions saleCreate = buildPermission("SALE_CREATE");
        Permissions employeRead = buildPermission("EMPLOYE_READ");
        Role vendeur = buildRole("SELLER", "Vendeur de magasin", Set.of(saleCreate, employeRead));
        when(roleDomainService.findAllWithPermissions()).thenReturn(List.of(vendeur));

        UserPrincipal adminPrincipal = new UserPrincipal(
                UUID.randomUUID(), null, null, null, "admin", "XOF", "Sénégal",
                "ADMIN", List.of(PermissionCode.ADMIN_ACCESS.name())
        );
        when(currentUserService.getCurrent()).thenReturn(adminPrincipal);

        List<RoleResponse> result = service.findAllScoped();

        assertThat(result).hasSize(1);
        RoleResponse projected = result.get(0);
        assertThat(projected.libelle()).isEqualTo("SELLER");
        assertThat(projected.description()).isEqualTo("Vendeur de magasin");
        assertThat(projected.permissions()).containsExactly("EMPLOYE_READ", "SALE_CREATE");
    }

    private static Permissions buildPermission(String code) {
        Permissions permission = new Permissions();
        permission.setId(UUID.randomUUID());
        permission.setCode(code);
        return permission;
    }

    @Test
    void resolvePermissions_should_return_permissions_for_known_codes() {
        Permissions saleRead = buildPermission("SALE_READ");
        Permissions stockRead = buildPermission("STOCK_READ");
        when(permissionsDomainService.findByCode("SALE_READ")).thenReturn(Optional.of(saleRead));
        when(permissionsDomainService.findByCode("STOCK_READ")).thenReturn(Optional.of(stockRead));

        Set<Permissions> result = service.resolvePermissions(List.of("SALE_READ", "STOCK_READ"));

        assertThat(result).containsExactlyInAnyOrder(saleRead, stockRead);
    }

    @Test
    void resolvePermissions_should_ignore_unknown_codes() {
        Permissions saleRead = buildPermission("SALE_READ");
        when(permissionsDomainService.findByCode("SALE_READ")).thenReturn(Optional.of(saleRead));
        when(permissionsDomainService.findByCode("UNKNOWN_CODE")).thenReturn(Optional.empty());

        Set<Permissions> result = service.resolvePermissions(List.of("SALE_READ", "UNKNOWN_CODE"));

        assertThat(result).containsExactly(saleRead);
    }

    @Test
    void resolvePermissions_should_return_empty_set_for_empty_input() {
        Set<Permissions> result = service.resolvePermissions(List.of());
        assertThat(result).isEmpty();
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
