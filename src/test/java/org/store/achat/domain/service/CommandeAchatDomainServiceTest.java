package org.store.achat.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.achat.application.dto.CommandeAchatCreate;
import org.store.achat.application.dto.CommandeAchatFilter;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.MotifAnnulationAchat;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.repository.CommandeAchatRepository;
import org.store.magasin.domain.model.Magasin;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandeAchatDomainServiceTest {

    @Mock
    private CommandeAchatRepository repository;

    @InjectMocks
    private CommandeAchatDomainService service;

    private Fournisseur fournisseur;
    private Magasin magasin;
    private UUID entrepriseId;

    @BeforeEach
    void setUp() {
        fournisseur = new Fournisseur();
        fournisseur.setId(UUID.randomUUID());

        magasin = new Magasin();
        magasin.setId(UUID.randomUUID());

        entrepriseId = UUID.randomUUID();
    }

    @Test
    void create_should_map_all_fields_and_return_saved_commande() {
        LocalDate dateCommande = LocalDate.of(2025, 6, 1);
        CommandeAchatCreate createCmd = new CommandeAchatCreate(
                fournisseur, magasin, dateCommande, "CMD-20250601-123456789", CommandeAchatStatut.DRAFT);

        when(repository.save(any(CommandeAchat.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandeAchat result = service.create(createCmd);

        ArgumentCaptor<CommandeAchat> captor = ArgumentCaptor.forClass(CommandeAchat.class);
        verify(repository).save(captor.capture());
        CommandeAchat saved = captor.getValue();

        assertThat(saved.getFournisseur()).isSameAs(fournisseur);
        assertThat(saved.getMagasin()).isSameAs(magasin);
        assertThat(saved.getDate()).isEqualTo(dateCommande);
        assertThat(saved.getReference()).isEqualTo("CMD-20250601-123456789");
        assertThat(saved.getStatut()).isEqualTo(CommandeAchatStatut.DRAFT);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void generateReference_should_return_string_starting_with_CMD() {
        String reference = service.generateReference();

        assertThat(reference).startsWith("CMD-");
        assertThat(reference).isNotBlank();
    }

    @Test
    void generateReference_should_match_expected_format() {
        String ref = service.generateReference();
        assertThat(ref).matches("CMD-\\d{8}-\\d{9}");
    }

    @Test
    void findResponsesByFilter_should_delegate_to_repository_with_filter_and_entreprise() {
        UUID magasinId = magasin.getId();
        CommandeAchatFilter filter = new CommandeAchatFilter(
                magasinId, null, null, null, null, null, null, 0, 10);

        CommandeAchatResponse responseItem = new CommandeAchatResponse(buildCommande("CMD-001"));
        Page<CommandeAchatResponse> page = new PageImpl<>(
                List.of(responseItem), PageRequest.of(0, 10), 1);

        when(repository.findResponsesByFilter(
                eq(entrepriseId), eq(filter.magasinId()), eq(filter.fournisseurId()),
                eq(filter.statutAsEnum()), eq(filter.statutFactureAsEnum()),
                eq(filter.reference()), eq(filter.startDate()), eq(filter.endDate()),
                eq(filter.toPageable())))
                .thenReturn(page);

        Page<CommandeAchatResponse> result = service.findResponsesByFilter(filter, entrepriseId);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(repository).findResponsesByFilter(
                entrepriseId, filter.magasinId(), filter.fournisseurId(),
                filter.statutAsEnum(), filter.statutFactureAsEnum(),
                filter.reference(), filter.startDate(), filter.endDate(),
                filter.toPageable());
    }

    @Test
    void markReceptionnee_should_set_statut_to_receptionnee_and_save() {
        CommandeAchat commande = buildCommande("CMD-RECEP-001");
        commande.setStatut(CommandeAchatStatut.DRAFT);

        when(repository.save(any(CommandeAchat.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandeAchat result = service.markReceptionnee(commande);

        assertThat(result.getStatut()).isEqualTo(CommandeAchatStatut.RECEPTIONNEE);
        verify(repository).save(commande);
    }

    @Test
    void cancel_should_set_statut_motif_commentaire_and_dateAnnulation() {
        CommandeAchat commande = buildCommande("CMD-CANCEL-001");
        commande.setStatut(CommandeAchatStatut.DRAFT);

        when(repository.save(any(CommandeAchat.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandeAchat result = service.cancel(
                commande, MotifAnnulationAchat.ERREUR_SAISIE, "Mauvaise saisie");

        assertThat(result.getStatut()).isEqualTo(CommandeAchatStatut.ANNULEE);
        assertThat(result.getMotifAnnulation()).isEqualTo(MotifAnnulationAchat.ERREUR_SAISIE);
        assertThat(result.getCommentaireAnnulation()).isEqualTo("Mauvaise saisie");
        assertThat(result.getDateAnnulation()).isNotNull();

        verify(repository).save(commande);
    }

    @Test
    void cancel_should_set_null_commentaire_when_not_provided() {
        CommandeAchat commande = buildCommande("CMD-CANCEL-002");

        when(repository.save(any(CommandeAchat.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandeAchat result = service.cancel(commande, MotifAnnulationAchat.AUTRE, null);

        assertThat(result.getStatut()).isEqualTo(CommandeAchatStatut.ANNULEE);
        assertThat(result.getMotifAnnulation()).isEqualTo(MotifAnnulationAchat.AUTRE);
        assertThat(result.getCommentaireAnnulation()).isNull();
        assertThat(result.getDateAnnulation()).isNotNull();
    }

    private CommandeAchat buildCommande(String reference) {
        CommandeAchat commande = new CommandeAchat();
        commande.setId(UUID.randomUUID());
        commande.setReference(reference);
        commande.setFournisseur(fournisseur);
        commande.setMagasin(magasin);
        commande.setDate(LocalDate.now());
        commande.setStatut(CommandeAchatStatut.DRAFT);
        return commande;
    }
}
