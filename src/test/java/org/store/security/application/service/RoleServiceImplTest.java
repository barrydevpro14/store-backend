package org.store.security.application.service;

import org.store.security.application.dto.RoleResponse;
import org.store.security.application.service.impl.RoleServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.EntityException;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
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

    @Mock
    private RoleDomainService roleDomainService;

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
    void findAll_should_project_each_role_with_permissions_sorted_by_code() {
        Permissions saleCreate = buildPermission("SALE_CREATE");
        Permissions employeRead = buildPermission("EMPLOYE_READ");
        Role vendeur = buildRole("SELLER", "Vendeur de magasin", Set.of(saleCreate, employeRead));
        when(roleDomainService.findAll()).thenReturn(List.of(vendeur));

        List<RoleResponse> result = service.findAll();

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

    private static Role buildRole(String libelle, String description, Set<Permissions> permissions) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setLibelle(libelle);
        role.setDescription(description);
        role.setPermissions(new LinkedHashSet<>(permissions));
        return role;
    }
}
