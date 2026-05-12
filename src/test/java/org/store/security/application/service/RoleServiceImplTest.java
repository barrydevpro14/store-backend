package org.store.security.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.EntityException;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.RoleRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl service;

    @Test
    void should_return_role_when_libelle_exists() {
        Role role = new Role();
        when(roleRepository.findByLibelle("PROPRIETAIRE")).thenReturn(Optional.of(role));

        Role result = service.findByLibelle("PROPRIETAIRE");

        assertThat(result).isSameAs(role);
    }

    @Test
    void should_throw_entity_exception_when_libelle_unknown() {
        when(roleRepository.findByLibelle("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByLibelle("GHOST"))
                .isInstanceOf(EntityException.class);
    }
}
