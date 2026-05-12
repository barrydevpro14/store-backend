package org.store.security.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.security.domain.service.PermissionsDomainService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionsServiceImplTest {

    @Mock
    private PermissionsDomainService permissionsDomainService;

    @InjectMocks
    private PermissionsServiceImpl service;

    @Test
    void should_return_codes_for_given_role() {
        UUID roleId = UUID.randomUUID();
        when(permissionsDomainService.findAllByRoleId(roleId)).thenReturn(List.of("EMPLOYE_ACCESS", "EMPLOYE_CREATE"));

        List<String> result = service.findAllByRoleId(roleId);

        assertThat(result).containsExactly("EMPLOYE_ACCESS", "EMPLOYE_CREATE");
    }

    @Test
    void should_return_empty_when_role_has_no_permissions() {
        UUID roleId = UUID.randomUUID();
        when(permissionsDomainService.findAllByRoleId(roleId)).thenReturn(List.of());

        List<String> result = service.findAllByRoleId(roleId);

        assertThat(result).isEmpty();
    }
}
