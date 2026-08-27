package org.store.abonnement.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.domain.enums.StatutPreuvePaiement;
import org.store.abonnement.domain.model.PreuvePaiement;
import org.store.abonnement.domain.repository.PreuvePaiementRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreuvePaiementDomainServiceTest {

    @Mock private PreuvePaiementRepository repository;

    @InjectMocks
    private PreuvePaiementDomainService service;

    @Test
    void existsPendingForFacture_should_delegate_to_repository() {
        UUID factureId = UUID.randomUUID();
        when(repository.existsByPaiementAbonnementIdAndStatut(factureId, StatutPreuvePaiement.EN_ATTENTE_VALIDATION))
                .thenReturn(true);

        boolean result = service.existsPendingForFacture(factureId);

        assertThat(result).isTrue();
    }

    @Test
    void findByFactureId_should_delegate_to_repository() {
        UUID factureId = UUID.randomUUID();
        List<PreuvePaiement> expected = List.of(new PreuvePaiement());
        when(repository.findByPaiementAbonnementIdOrderByCreatedAtDesc(factureId)).thenReturn(expected);

        List<PreuvePaiement> result = service.findByFactureId(factureId);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void markAsValidee_should_set_statut_and_save() {
        PreuvePaiement preuve = new PreuvePaiement();
        when(repository.save(preuve)).thenReturn(preuve);

        PreuvePaiement result = service.markAsValidee(preuve);

        assertThat(result.getStatut()).isEqualTo(StatutPreuvePaiement.VALIDEE);
        verify(repository).save(preuve);
    }

    @Test
    void markAsRejetee_should_set_statut_and_motif_and_save() {
        PreuvePaiement preuve = new PreuvePaiement();
        when(repository.save(preuve)).thenReturn(preuve);

        PreuvePaiement result = service.markAsRejetee(preuve, "Preuve illisible");

        assertThat(result.getStatut()).isEqualTo(StatutPreuvePaiement.REJETEE);
        assertThat(result.getMotifRejet()).isEqualTo("Preuve illisible");
        verify(repository).save(preuve);
    }
}
