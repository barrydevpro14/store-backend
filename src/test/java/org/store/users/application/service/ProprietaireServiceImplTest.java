package org.store.users.application.service;

import org.store.users.application.service.impl.ProprietaireServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.security.domain.model.Account;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.service.ProprietaireDomainService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProprietaireServiceImplTest {

    @Mock
    private ProprietaireDomainService proprietaireDomainService;

    @InjectMocks
    private ProprietaireServiceImpl service;

    @Test
    void create_should_delegate_to_domain_service() {
        UtilisateurRequest request = new UtilisateurRequest(
                "Doe", "John", "john@example.com", "+221770000000", "Dakar"
        );
        Account account = new Account();
        Proprietaire expected = new Proprietaire();
        when(proprietaireDomainService.create(request, account)).thenReturn(expected);

        Proprietaire result = service.create(request, account);

        assertThat(result).isSameAs(expected);
    }
}
