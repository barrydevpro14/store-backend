package org.store.users.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.UniqueResourceException;
import org.store.users.domain.repository.UtilisateurRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilisateurDomainServiceTest {

    @Mock
    private UtilisateurRepository repository;

    @InjectMocks
    private UtilisateurDomainService service;

    @Test
    void ensureContactsAvailable_should_pass_when_email_and_telephone_are_free() {
        when(repository.existsByEmail("new@example.com")).thenReturn(false);
        when(repository.existsByTelephone("+221770000000")).thenReturn(false);

        assertThatCode(() -> service.ensureContactsAvailable("new@example.com", "+221770000000"))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureContactsAvailable_should_throw_with_email_key_when_email_is_taken() {
        when(repository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.ensureContactsAvailable("dup@example.com", "+221770000000"))
                .isInstanceOf(UniqueResourceException.class)
                .hasMessageContaining("utilisateur.email.alreadyExists");
    }

    @Test
    void ensureContactsAvailable_should_throw_with_telephone_key_when_only_telephone_is_taken() {
        when(repository.existsByEmail("new@example.com")).thenReturn(false);
        when(repository.existsByTelephone("+221770000000")).thenReturn(true);

        assertThatThrownBy(() -> service.ensureContactsAvailable("new@example.com", "+221770000000"))
                .isInstanceOf(UniqueResourceException.class)
                .hasMessageContaining("utilisateur.telephone.alreadyExists");
    }

    @Test
    void ensureContactsAvailableForUpdate_should_pass_when_only_the_current_user_owns_the_contacts() {
        UUID currentUserId = UUID.randomUUID();
        when(repository.existsByEmailAndIdNot("same@example.com", currentUserId)).thenReturn(false);
        when(repository.existsByTelephoneAndIdNot("+221770000000", currentUserId)).thenReturn(false);

        assertThatCode(() -> service.ensureContactsAvailableForUpdate("same@example.com", "+221770000000", currentUserId))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureContactsAvailableForUpdate_should_throw_email_alreadyExists_when_another_row_owns_the_email() {
        UUID currentUserId = UUID.randomUUID();
        when(repository.existsByEmailAndIdNot("taken@example.com", currentUserId)).thenReturn(true);

        assertThatThrownBy(() -> service.ensureContactsAvailableForUpdate("taken@example.com", "+221770000000", currentUserId))
                .isInstanceOf(UniqueResourceException.class)
                .hasMessageContaining("utilisateur.email.alreadyExists");
    }

    @Test
    void ensureContactsAvailableForUpdate_should_throw_telephone_alreadyExists_when_only_the_telephone_collides() {
        UUID currentUserId = UUID.randomUUID();
        when(repository.existsByEmailAndIdNot("ok@example.com", currentUserId)).thenReturn(false);
        when(repository.existsByTelephoneAndIdNot("+221770000000", currentUserId)).thenReturn(true);

        assertThatThrownBy(() -> service.ensureContactsAvailableForUpdate("ok@example.com", "+221770000000", currentUserId))
                .isInstanceOf(UniqueResourceException.class)
                .hasMessageContaining("utilisateur.telephone.alreadyExists");
    }
}
