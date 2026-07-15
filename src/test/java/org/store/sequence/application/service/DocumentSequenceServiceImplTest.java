package org.store.sequence.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.sequence.application.dto.DocumentSequenceRequest;
import org.store.sequence.application.dto.DocumentSequenceResponse;
import org.store.sequence.application.dto.DocumentSequenceUpdateRequest;
import org.store.sequence.application.service.impl.DocumentSequenceServiceImpl;
import org.store.sequence.domain.enums.TypeDocument;
import org.store.sequence.domain.model.DocumentSequence;
import org.store.sequence.domain.service.DocumentSequenceDomainService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentSequenceServiceImplTest {

    @Mock private DocumentSequenceDomainService domainService;
    @Mock private IMagasinService magasinService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private DocumentSequenceServiceImpl service;

    private UUID magasinId;
    private Magasin magasin;

    @BeforeEach
    void setUp() {
        magasinId = UUID.randomUUID();
        magasin = new Magasin();
        magasin.setId(magasinId);
    }

    @Test
    void generateReference_should_return_structured_format_when_sequence_configured() {
        DocumentSequence seq = buildSeq(TypeDocument.FACTURE_CLIENT, "FV", 1L, 6);
        when(domainService.findForUpdate(magasinId, TypeDocument.FACTURE_CLIENT)).thenReturn(Optional.of(seq));
        when(domainService.save(seq)).thenReturn(seq);

        String reference = service.generateReference(magasinId, TypeDocument.FACTURE_CLIENT);

        int year = LocalDate.now().getYear();
        assertThat(reference).isEqualTo("FV-" + year + "-000001");
        assertThat(seq.getProchaineSequence()).isEqualTo(2L);
    }

    @Test
    void generateReference_should_not_truncate_when_sequence_exceeds_longueur() {
        DocumentSequence seq = buildSeq(TypeDocument.FACTURE_CLIENT, "FV", 1234567L, 6);
        when(domainService.findForUpdate(magasinId, TypeDocument.FACTURE_CLIENT)).thenReturn(Optional.of(seq));
        when(domainService.save(seq)).thenReturn(seq);

        String reference = service.generateReference(magasinId, TypeDocument.FACTURE_CLIENT);

        int year = LocalDate.now().getYear();
        assertThat(reference).isEqualTo("FV-" + year + "-1234567");
    }

    @Test
    void generateReference_should_fallback_to_timestamp_when_no_sequence_configured() {
        when(domainService.findForUpdate(magasinId, TypeDocument.COMMANDE_CLIENT)).thenReturn(Optional.empty());

        String reference = service.generateReference(magasinId, TypeDocument.COMMANDE_CLIENT);

        assertThat(reference).startsWith("VTE-");
    }

    @Test
    void generateReference_should_increment_prochaine_sequence_after_generation() {
        DocumentSequence seq = buildSeq(TypeDocument.COMMANDE_ACHAT, "CA", 5L, 6);
        when(domainService.findForUpdate(magasinId, TypeDocument.COMMANDE_ACHAT)).thenReturn(Optional.of(seq));
        when(domainService.save(seq)).thenReturn(seq);

        service.generateReference(magasinId, TypeDocument.COMMANDE_ACHAT);

        assertThat(seq.getProchaineSequence()).isEqualTo(6L);
        verify(domainService).save(seq);
    }

    @Test
    void create_should_persist_and_return_response() {
        DocumentSequenceRequest request = new DocumentSequenceRequest(
                magasinId, "FACTURE_CLIENT", "FV", 1L, 6);

        DocumentSequence saved = buildSeq(TypeDocument.FACTURE_CLIENT, "FV", 1L, 6);
        saved.setId(UUID.randomUUID());

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(domainService.existsByMagasinIdAndTypeDocument(magasinId, TypeDocument.FACTURE_CLIENT)).thenReturn(false);
        when(domainService.save(any(DocumentSequence.class))).thenReturn(saved);

        DocumentSequenceResponse response = service.create(request);

        assertThat(response.magasinId()).isEqualTo(magasinId);
        assertThat(response.typeDocument()).isEqualTo(TypeDocument.FACTURE_CLIENT);
        assertThat(response.prefixe()).isEqualTo("FV");
    }

    @Test
    void create_should_throw_when_sequence_already_exists() {
        DocumentSequenceRequest request = new DocumentSequenceRequest(
                magasinId, "FACTURE_CLIENT", "FV", 1L, 6);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(domainService.existsByMagasinIdAndTypeDocument(magasinId, TypeDocument.FACTURE_CLIENT)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UniqueResourceException.class);
    }

    @Test
    void update_should_apply_new_values_and_return_response() {
        DocumentSequence seq = buildSeq(TypeDocument.FACTURE_CLIENT, "FV", 1L, 6);
        seq.setId(UUID.randomUUID());
        seq.setMagasinId(magasinId);

        DocumentSequenceUpdateRequest request = new DocumentSequenceUpdateRequest("FAC", 100L, 8);

        when(domainService.findById(seq.getId())).thenReturn(seq);
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(domainService.save(seq)).thenReturn(seq);

        DocumentSequenceResponse response = service.update(seq.getId(), request);

        assertThat(response.prefixe()).isEqualTo("FAC");
        assertThat(response.prochaineSequence()).isEqualTo(100L);
        assertThat(response.longueurSequence()).isEqualTo(8);
    }

    @Test
    void delete_should_remove_sequence() {
        DocumentSequence seq = buildSeq(TypeDocument.FACTURE_ACHAT, "FA", 1L, 6);
        seq.setId(UUID.randomUUID());
        seq.setMagasinId(magasinId);

        when(domainService.findById(seq.getId())).thenReturn(seq);
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);

        service.delete(seq.getId());

        verify(domainService).delete(seq);
    }

    private DocumentSequence buildSeq(TypeDocument type, String prefixe, long prochaineSequence, int longueur) {
        DocumentSequence seq = new DocumentSequence();
        seq.setMagasinId(magasinId);
        seq.setTypeDocument(type);
        seq.setPrefixe(prefixe);
        seq.setProchaineSequence(prochaineSequence);
        seq.setLongueurSequence(longueur);
        return seq;
    }
}
