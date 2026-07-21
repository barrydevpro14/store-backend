package org.store.inventaire.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.entreprise.domain.model.Entreprise;
import org.store.inventaire.application.dto.RapportInventaireCommand;
import org.store.inventaire.application.dto.RapportInventaireResponse;
import org.store.inventaire.application.service.impl.RapportInventaireServiceImpl;
import org.store.inventaire.domain.enums.InventaireStatut;
import org.store.inventaire.domain.enums.StatutRapport;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.service.RapportInventaireDomainService;
import org.store.magasin.domain.model.Magasin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RapportInventaireServiceImplTest {

    @Mock private RapportInventaireDomainService rapportInventaireDomainService;

    @InjectMocks
    private RapportInventaireServiceImpl service;

    private UUID inventaireId;
    private UUID entrepriseId;
    private Inventaire inventaire;

    @BeforeEach
    void setUp() {
        inventaireId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        Magasin magasin = new Magasin();
        magasin.setId(UUID.randomUUID());
        magasin.setEntreprise(entreprise);

        inventaire = new Inventaire();
        inventaire.setId(inventaireId);
        inventaire.setMagasin(magasin);
        inventaire.setStatut(InventaireStatut.BILAN);
        inventaire.setDate(LocalDate.now());
    }

    @Test
    void create_should_delegate_to_domain_service_with_command() {
        RapportInventaireCommand command = new RapportInventaireCommand(
                new BigDecimal("1000.00"), new BigDecimal("1100.00"),
                new BigDecimal("500.00"), new BigDecimal("200.00"),
                new BigDecimal("300.00"),
                LocalDate.now().minusDays(7), LocalDate.now()
        );

        service.create(inventaire, command);

        ArgumentCaptor<RapportInventaireCommand> captor = ArgumentCaptor.forClass(RapportInventaireCommand.class);
        verify(rapportInventaireDomainService).create(org.mockito.ArgumentMatchers.eq(inventaire), captor.capture());
        assertThat(captor.getValue().montantAutomatique()).isEqualByComparingTo("1000.00");
        assertThat(captor.getValue().montantPhysique()).isEqualByComparingTo("1100.00");
    }

    @Test
    void findResponseByInventaireId_should_return_present_when_rapport_exists() {
        RapportInventaireResponse response = new RapportInventaireResponse(
                UUID.randomUUID(), inventaireId,
                new BigDecimal("1000.00"), new BigDecimal("1100.00"),
                new BigDecimal("100.00"), new BigDecimal("500.00"), new BigDecimal("200.00"),
                new BigDecimal("300.00"), "2026-07-14", "2026-07-21",
                new BigDecimal("1100.00"), StatutRapport.BENEFICE, null
        );
        when(rapportInventaireDomainService.findResponseByInventaireId(inventaireId, entrepriseId))
                .thenReturn(Optional.of(response));

        Optional<RapportInventaireResponse> result = service.findResponseByInventaireId(inventaireId, entrepriseId);

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(StatutRapport.BENEFICE);
        verify(rapportInventaireDomainService).findResponseByInventaireId(inventaireId, entrepriseId);
    }

    @Test
    void findResponseByInventaireId_should_return_empty_when_absent() {
        when(rapportInventaireDomainService.findResponseByInventaireId(inventaireId, entrepriseId))
                .thenReturn(Optional.empty());

        assertThat(service.findResponseByInventaireId(inventaireId, entrepriseId)).isEmpty();
    }
}
