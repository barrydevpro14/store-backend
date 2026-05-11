package org.store.users.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.security.domain.model.Account;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.repository.ProprietaireRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProprietaireServiceImplTest {

    @Mock
    private ProprietaireRepository proprietaireRepository;

    @InjectMocks
    private ProprietaireServiceImpl service;

    @Test
    void should_create_proprietaire_with_account_and_personal_info() {
        UtilisateurRequest request = new UtilisateurRequest(
                "Doe", "John", "john@example.com", "+221700000000", "Dakar"
        );
        Account account = new Account();
        when(proprietaireRepository.save(any(Proprietaire.class))).thenAnswer(inv -> inv.getArgument(0));

        Proprietaire result = service.create(request, account);

        ArgumentCaptor<Proprietaire> captor = ArgumentCaptor.forClass(Proprietaire.class);
        verify(proprietaireRepository).save(captor.capture());
        Proprietaire saved = captor.getValue();
        assertThat(saved.getAccount()).isSameAs(account);
        assertThat(saved.getNom()).isEqualTo("Doe");
        assertThat(saved.getPrenom()).isEqualTo("John");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getTelephone()).isEqualTo("+221700000000");
        assertThat(saved.getAdresse()).isEqualTo("Dakar");
        assertThat(result).isSameAs(saved);
    }
}
