package org.store.users.application.service;

import org.store.users.application.service.impl.ProprietaireServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.UniqueResourceException;
import org.store.security.domain.model.Account;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.service.ProprietaireDomainService;
import org.store.users.domain.service.UtilisateurDomainService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProprietaireServiceImplTest {

    @Mock
    private ProprietaireDomainService proprietaireDomainService;

    @Mock
    private UtilisateurDomainService utilisateurDomainService;

    @InjectMocks
    private ProprietaireServiceImpl service;

    @Test
    void create_should_check_contacts_uniqueness_then_delegate_to_domain_service() {
        UtilisateurRequest request = new UtilisateurRequest(
                "Doe", "John", "john@example.com", "+221770000000", "Dakar"
        );
        Account account = new Account();
        Proprietaire expected = new Proprietaire();
        when(proprietaireDomainService.create(request, account)).thenReturn(expected);

        Proprietaire result = service.create(request, account);

        assertThat(result).isSameAs(expected);
        InOrder ordered = inOrder(utilisateurDomainService, proprietaireDomainService);
        ordered.verify(utilisateurDomainService).ensureContactsAvailable("john@example.com", "+221770000000");
        ordered.verify(proprietaireDomainService).create(request, account);
    }

    @Test
    void create_should_propagate_unique_resource_exception_and_skip_domain_create() {
        UtilisateurRequest request = new UtilisateurRequest(
                "Doe", "John", "duplicate@example.com", "+221770000000", "Dakar"
        );
        Account account = new Account();
        doThrow(new UniqueResourceException("utilisateur.email.alreadyExists", "duplicate@example.com"))
                .when(utilisateurDomainService).ensureContactsAvailable("duplicate@example.com", "+221770000000");

        assertThatThrownBy(() -> service.create(request, account))
                .isInstanceOf(UniqueResourceException.class);

        org.mockito.Mockito.verify(proprietaireDomainService, never()).create(request, account);
    }
}
