package org.store.magasin.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.domain.model.Magasin;
import org.store.magasin.domain.repository.MagasinRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MagasinDomainServiceTest {

    @Mock
    private MagasinRepository repository;

    @InjectMocks
    private MagasinDomainService service;

    @Test
    void create_should_construct_magasin_with_entreprise_and_active() {
        MagasinRequest request = new MagasinRequest("Magasin Centre", "Dakar Centre");
        Entreprise entreprise = new Entreprise();
        when(repository.save(any(Magasin.class))).thenAnswer(inv -> inv.getArgument(0));

        Magasin result = service.create(request, entreprise);

        ArgumentCaptor<Magasin> captor = ArgumentCaptor.forClass(Magasin.class);
        verify(repository).save(captor.capture());
        Magasin saved = captor.getValue();
        assertThat(saved.getEntreprise()).isSameAs(entreprise);
        assertThat(saved.getNom()).isEqualTo("Magasin Centre");
        assertThat(saved.getAdresse()).isEqualTo("Dakar Centre");
        assertThat(saved.isActif()).isTrue();
        assertThat(result).isSameAs(saved);
    }
}
