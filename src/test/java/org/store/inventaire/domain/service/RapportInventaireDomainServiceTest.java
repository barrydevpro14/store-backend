package org.store.inventaire.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.inventaire.application.dto.RapportInventaireCommand;
import org.store.inventaire.domain.enums.StatutRapport;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.model.RapportInventaire;
import org.store.inventaire.domain.repository.RapportInventaireRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RapportInventaireDomainServiceTest {

    @Mock private RapportInventaireRepository repository;

    @InjectMocks
    private RapportInventaireDomainService service;

    private Inventaire inventaire;

    @BeforeEach
    void setUp() {
        inventaire = new Inventaire();
        inventaire.setId(UUID.randomUUID());
        when(repository.save(any(RapportInventaire.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private RapportInventaireCommand command(String automatique, String physique, String caisse, String depense, String roulement) {
        return new RapportInventaireCommand(
                new BigDecimal(automatique), new BigDecimal(physique),
                new BigDecimal(caisse), new BigDecimal(depense), new BigDecimal(roulement),
                LocalDate.now().minusDays(7), LocalDate.now()
        );
    }

    @Test
    void create_should_compute_ecart_benefice_and_BENEFICE_status() {
        RapportInventaire rapport = service.create(inventaire, command("100.00", "120.00", "500.00", "50.00", "400.00"));

        assertThat(rapport.getEcart()).isEqualByComparingTo("20.00");
        assertThat(rapport.getBenefice()).isEqualByComparingTo("170.00");
        assertThat(rapport.getStatus()).isEqualTo(StatutRapport.BENEFICE);
    }

    @Test
    void create_should_assign_PERTE_status_when_benefice_negative() {
        RapportInventaire rapport = service.create(inventaire, command("100.00", "50.00", "100.00", "300.00", "500.00"));

        assertThat(rapport.getBenefice()).isEqualByComparingTo("-650.00");
        assertThat(rapport.getStatus()).isEqualTo(StatutRapport.PERTE);
    }

    @Test
    void create_should_assign_EQUILIBRE_status_when_benefice_zero() {
        RapportInventaire rapport = service.create(inventaire, command("100.00", "100.00", "100.00", "100.00", "100.00"));

        assertThat(rapport.getBenefice()).isEqualByComparingTo("0.00");
        assertThat(rapport.getStatus()).isEqualTo(StatutRapport.EQUILIBRE);
    }

    @Test
    void create_should_propagate_command_fields_to_entity() {
        LocalDate dateDebut = LocalDate.now().minusDays(10);
        LocalDate dateFin = LocalDate.now();
        RapportInventaireCommand command = new RapportInventaireCommand(
                new BigDecimal("200.00"), new BigDecimal("250.00"),
                new BigDecimal("80.00"), new BigDecimal("30.00"), new BigDecimal("150.00"),
                dateDebut, dateFin
        );

        service.create(inventaire, command);

        ArgumentCaptor<RapportInventaire> captor = ArgumentCaptor.forClass(RapportInventaire.class);
        verify(repository).save(captor.capture());
        RapportInventaire saved = captor.getValue();
        assertThat(saved.getInventaire()).isEqualTo(inventaire);
        assertThat(saved.getMontantAutomatique()).isEqualByComparingTo("200.00");
        assertThat(saved.getMontantPhysique()).isEqualByComparingTo("250.00");
        assertThat(saved.getMontantCaisse()).isEqualByComparingTo("80.00");
        assertThat(saved.getDepense()).isEqualByComparingTo("30.00");
        assertThat(saved.getMontantRoulement()).isEqualByComparingTo("150.00");
        assertThat(saved.getDateDebutPeriode()).isEqualTo(dateDebut);
        assertThat(saved.getDateFinPeriode()).isEqualTo(dateFin);
    }
}
