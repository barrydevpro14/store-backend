package org.store.vente.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.domain.enums.StatutFacture;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.repository.FactureClientRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FactureClientDomainServiceTest {

    @Mock private FactureClientRepository factureClientRepository;

    @InjectMocks
    private FactureClientDomainService factureClientDomainService;

    private FactureClient facture;

    @BeforeEach
    void setUp() {
        facture = new FactureClient();
        facture.setId(UUID.randomUUID());
        facture.setNumero("FAC-VTE-AUTO");
        facture.setMontantTotal(new BigDecimal("1000.00"));
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setStatut(StatutFacture.NON_PAYEE);
    }

    @Test
    void applyPaiement_should_transition_to_partiellement_payee_when_below_total() {
        when(factureClientRepository.save(any(FactureClient.class))).thenAnswer(inv -> inv.getArgument(0));

        FactureClient updated = factureClientDomainService.applyPaiement(facture, new BigDecimal("400.00"));

        assertThat(updated.getMontantPaye()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(updated.getStatut()).isEqualTo(StatutFacture.PARTIELLEMENT_PAYEE);
    }

    @Test
    void applyPaiement_should_transition_to_payee_when_equals_total() {
        when(factureClientRepository.save(any(FactureClient.class))).thenAnswer(inv -> inv.getArgument(0));

        FactureClient updated = factureClientDomainService.applyPaiement(facture, new BigDecimal("1000.00"));

        assertThat(updated.getMontantPaye()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(updated.getStatut()).isEqualTo(StatutFacture.PAYEE);
    }

    @Test
    void applyPaiement_should_transition_to_payee_when_above_total() {
        when(factureClientRepository.save(any(FactureClient.class))).thenAnswer(inv -> inv.getArgument(0));

        FactureClient updated = factureClientDomainService.applyPaiement(facture, new BigDecimal("1200.00"));

        assertThat(updated.getMontantPaye()).isEqualByComparingTo(new BigDecimal("1200.00"));
        assertThat(updated.getStatut()).isEqualTo(StatutFacture.PAYEE);
    }

    @Test
    void applyPaiement_should_accumulate_with_existing_montantPaye() {
        facture.setMontantPaye(new BigDecimal("300.00"));
        facture.setStatut(StatutFacture.PARTIELLEMENT_PAYEE);
        when(factureClientRepository.save(any(FactureClient.class))).thenAnswer(inv -> inv.getArgument(0));

        FactureClient updated = factureClientDomainService.applyPaiement(facture, new BigDecimal("700.00"));

        assertThat(updated.getMontantPaye()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(updated.getStatut()).isEqualTo(StatutFacture.PAYEE);
    }

    @Test
    void applyPaiement_should_default_null_montantPaye_to_zero() {
        facture.setMontantPaye(null);
        when(factureClientRepository.save(any(FactureClient.class))).thenAnswer(inv -> inv.getArgument(0));

        FactureClient updated = factureClientDomainService.applyPaiement(facture, new BigDecimal("500.00"));

        assertThat(updated.getMontantPaye()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(updated.getStatut()).isEqualTo(StatutFacture.PARTIELLEMENT_PAYEE);
    }
}
