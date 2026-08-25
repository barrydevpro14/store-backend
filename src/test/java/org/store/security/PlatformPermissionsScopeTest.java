package org.store.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.store.property.RbacProperties;
import org.store.security.application.dto.RbacConfig;
import org.store.security.application.dto.RbacConfig.RoleDef;
import org.store.security.application.service.impl.RolesPermissionsSyncServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the feature's core security boundary: PLATFORM_* permissions (platform expenses,
 * expense categories, and the platform P&L report) must stay ADMIN-only in the RBAC seed.
 * A future edit of roles-permissions.yml granting one of these to OWNER or MANAGER
 * would fail this test.
 */
class PlatformPermissionsScopeTest {

    private static final List<String> PLATFORM_PERMISSIONS = List.of(
            "PLATFORM_EXPENSE_CREATE",
            "PLATFORM_EXPENSE_READ",
            "PLATFORM_EXPENSE_UPDATE",
            "PLATFORM_EXPENSE_DELETE",
            "PLATFORM_EXPENSE_CATEGORY_CREATE",
            "PLATFORM_EXPENSE_CATEGORY_READ",
            "PLATFORM_EXPENSE_CATEGORY_UPDATE",
            "PLATFORM_EXPENSE_CATEGORY_DELETE",
            "PLATFORM_REPORT_READ"
    );

    private final RbacConfig config = loadRbacConfig();

    @ParameterizedTest
    @ValueSource(strings = {
            "PLATFORM_EXPENSE_CREATE", "PLATFORM_EXPENSE_READ", "PLATFORM_EXPENSE_UPDATE", "PLATFORM_EXPENSE_DELETE",
            "PLATFORM_EXPENSE_CATEGORY_CREATE", "PLATFORM_EXPENSE_CATEGORY_READ",
            "PLATFORM_EXPENSE_CATEGORY_UPDATE", "PLATFORM_EXPENSE_CATEGORY_DELETE",
            "PLATFORM_REPORT_READ"
    })
    void platform_permission_is_granted_to_admin_only(String permissionCode) {
        assertThat(role("ADMIN").permissions()).contains(permissionCode);
        assertThat(role("OWNER").permissions()).doesNotContain(permissionCode);
        assertThat(role("MANAGER").permissions()).doesNotContain(permissionCode);
    }

    @Test
    void platform_permissions_list_is_exhaustive_in_admin_role() {
        assertThat(role("ADMIN").permissions()).containsAll(PLATFORM_PERMISSIONS);
    }

    /** Resolves a RoleDef from the YAML config by libelle, failing loudly if the role is missing. */
    private RoleDef role(String libelle) {
        return config.roles().stream()
                .filter(roleDef -> roleDef.libelle().equals(libelle))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Role not found in YAML: " + libelle));
    }

    /** Loads the real RBAC seed via the production parser, without touching the database. */
    private RbacConfig loadRbacConfig() {
        RbacProperties properties = new RbacProperties(false, new ClassPathResource("security/roles-permissions.yml"), null);
        RolesPermissionsSyncServiceImpl parser = new RolesPermissionsSyncServiceImpl(properties, null, null);
        return parser.loadConfig();
    }
}
