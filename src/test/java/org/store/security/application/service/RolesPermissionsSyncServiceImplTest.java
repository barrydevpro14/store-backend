package org.store.security.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.store.property.RbacProperties;
import org.store.security.application.dto.RbacSyncReport;
import org.store.security.application.service.impl.RolesPermissionsSyncServiceImpl;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.PermissionsDomainService;
import org.store.security.domain.service.RoleDomainService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolesPermissionsSyncServiceImplTest {

    private static final String YAML = """
            permissions:
              - AUTH_LOGIN
              - SALE_CREATE
              - EMPLOYE_ACCESS
            roles:
              - libelle: VENDEUR
                description: Vendeur d'un magasin
                permissions:
                  - AUTH_LOGIN
                  - SALE_CREATE
                  - EMPLOYE_ACCESS
            """;

    @Mock
    private PermissionsDomainService permissionsDomainService;

    @Mock
    private RoleDomainService roleDomainService;

    private RolesPermissionsSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        RbacProperties properties = new RbacProperties(true, new ByteArrayResource(YAML.getBytes()) {
            @Override
            public boolean exists() {
                return true;
            }
        });
        service = new RolesPermissionsSyncServiceImpl(properties, permissionsDomainService, roleDomainService);
    }

    @Test
    void should_create_missing_permissions_and_role() {
        when(permissionsDomainService.findByCode(any())).thenReturn(Optional.empty());
        when(permissionsDomainService.create(any())).thenAnswer(inv -> {
            Permissions p = new Permissions();
            p.setId(UUID.randomUUID());
            p.setCode(inv.getArgument(0));
            return p;
        });
        when(permissionsDomainService.findAll()).thenReturn(List.of());
        when(roleDomainService.findByLibelle("VENDEUR")).thenReturn(Optional.empty());
        when(roleDomainService.create(any(), any())).thenAnswer(inv -> {
            Role r = new Role();
            r.setId(UUID.randomUUID());
            r.setLibelle(inv.getArgument(0));
            r.setDescription(inv.getArgument(1));
            r.setPermissions(new LinkedHashSet<>());
            return r;
        });
        when(roleDomainService.findAll()).thenReturn(List.of());

        RbacSyncReport report = service.sync();

        assertThat(report.addedPermissions())
                .containsExactly("AUTH_LOGIN", "SALE_CREATE", "EMPLOYE_ACCESS");
        assertThat(report.addedRoles()).containsExactly("VENDEUR");
        assertThat(report.updatedRoles()).isEmpty();
        assertThat(report.orphanPermissions()).isEmpty();
        assertThat(report.orphanRoles()).isEmpty();

        verify(permissionsDomainService, times(3)).create(any());
        verify(roleDomainService, times(1)).create(any(), any());
    }

    @Test
    void should_be_idempotent_when_everything_already_in_db() {
        Permissions login = perm("AUTH_LOGIN");
        Permissions saleCreate = perm("SALE_CREATE");
        Permissions employeAccess = perm("EMPLOYE_ACCESS");

        when(permissionsDomainService.findByCode("AUTH_LOGIN")).thenReturn(Optional.of(login));
        when(permissionsDomainService.findByCode("SALE_CREATE")).thenReturn(Optional.of(saleCreate));
        when(permissionsDomainService.findByCode("EMPLOYE_ACCESS")).thenReturn(Optional.of(employeAccess));
        when(permissionsDomainService.findAll()).thenReturn(List.of(login, saleCreate, employeAccess));

        Role vendeur = role("VENDEUR", login, saleCreate, employeAccess);
        when(roleDomainService.findByLibelle("VENDEUR")).thenReturn(Optional.of(vendeur));
        when(roleDomainService.findAll()).thenReturn(List.of(vendeur));

        RbacSyncReport report = service.sync();

        assertThat(report.addedPermissions()).isEmpty();
        assertThat(report.addedRoles()).isEmpty();
        assertThat(report.updatedRoles()).isEmpty();
        assertThat(report.orphanPermissions()).isEmpty();
        assertThat(report.orphanRoles()).isEmpty();

        verify(permissionsDomainService, never()).save(any());
        verify(roleDomainService, never()).save(any());
    }

    @Test
    void should_add_missing_associations_to_existing_role() {
        Permissions login = perm("AUTH_LOGIN");
        Permissions saleCreate = perm("SALE_CREATE");
        Permissions employeAccess = perm("EMPLOYE_ACCESS");
        when(permissionsDomainService.findByCode("AUTH_LOGIN")).thenReturn(Optional.of(login));
        when(permissionsDomainService.findByCode("SALE_CREATE")).thenReturn(Optional.of(saleCreate));
        when(permissionsDomainService.findByCode("EMPLOYE_ACCESS")).thenReturn(Optional.of(employeAccess));
        when(permissionsDomainService.findAll()).thenReturn(List.of(login, saleCreate, employeAccess));

        Role vendeur = role("VENDEUR", login);
        when(roleDomainService.findByLibelle("VENDEUR")).thenReturn(Optional.of(vendeur));
        when(roleDomainService.findAll()).thenReturn(List.of(vendeur));
        AtomicReference<Role> savedRole = new AtomicReference<>();
        when(roleDomainService.save(any())).thenAnswer(inv -> {
            savedRole.set(inv.getArgument(0));
            return inv.getArgument(0);
        });

        RbacSyncReport report = service.sync();

        assertThat(report.addedRoles()).isEmpty();
        assertThat(report.updatedRoles()).containsExactly("VENDEUR");
        assertThat(savedRole.get().getPermissions())
                .extracting(Permissions::getCode)
                .containsExactly("AUTH_LOGIN", "SALE_CREATE", "EMPLOYE_ACCESS");
    }

    @Test
    void should_report_orphan_permissions_and_roles_in_db_without_deleting_them() {
        Permissions login = perm("AUTH_LOGIN");
        Permissions saleCreate = perm("SALE_CREATE");
        Permissions employeAccess = perm("EMPLOYE_ACCESS");
        Permissions orphanPerm = perm("LEGACY_FORGOTTEN");

        when(permissionsDomainService.findByCode("AUTH_LOGIN")).thenReturn(Optional.of(login));
        when(permissionsDomainService.findByCode("SALE_CREATE")).thenReturn(Optional.of(saleCreate));
        when(permissionsDomainService.findByCode("EMPLOYE_ACCESS")).thenReturn(Optional.of(employeAccess));
        when(permissionsDomainService.findAll())
                .thenReturn(List.of(login, saleCreate, employeAccess, orphanPerm));

        Role vendeur = role("VENDEUR", login, saleCreate, employeAccess);
        Role orphanRole = role("LEGACY_ROLE");
        when(roleDomainService.findByLibelle("VENDEUR")).thenReturn(Optional.of(vendeur));
        when(roleDomainService.findAll()).thenReturn(List.of(vendeur, orphanRole));

        RbacSyncReport report = service.sync();

        assertThat(report.orphanPermissions()).containsExactly("LEGACY_FORGOTTEN");
        assertThat(report.orphanRoles()).containsExactly("LEGACY_ROLE");
        verify(permissionsDomainService, never()).deleteById(any());
        verify(roleDomainService, never()).deleteById(any());
    }

    private Permissions perm(String code) {
        Permissions p = new Permissions();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        return p;
    }

    private Role role(String libelle, Permissions... perms) {
        Role r = new Role();
        r.setId(UUID.randomUUID());
        r.setLibelle(libelle);
        Set<Permissions> set = new LinkedHashSet<>();
        for (Permissions p : perms) {
            set.add(p);
        }
        r.setPermissions(set);
        return r;
    }
}
