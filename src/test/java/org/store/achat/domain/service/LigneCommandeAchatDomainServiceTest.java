package org.store.achat.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.application.dto.LigneCommandeAchatUpdate;
import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.achat.domain.repository.LigneCommandeAchatRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LigneCommandeAchatDomainServiceTest {

    @Mock private LigneCommandeAchatRepository repository;

    @InjectMocks
    private LigneCommandeAchatDomainService service;

    @Test
    void update_should_apply_all_fields_and_persist() {
        LigneCommandeAchat ligne = new LigneCommandeAchat();
        LigneCommandeAchatUpdate update = new LigneCommandeAchatUpdate(
                50,
                new BigDecimal("12.00"),
                new BigDecimal("25.00"),
                "LOT-2025",
                LocalDate.of(2026, 6, 30)
        );
        when(repository.save(any(LigneCommandeAchat.class))).thenAnswer(inv -> inv.getArgument(0));

        LigneCommandeAchat result = service.update(ligne, update);

        assertThat(result.getQuantite()).isEqualTo(50);
        assertThat(result.getPrixAchat()).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(result.getPrixVente()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(result.getNumeroLot()).isEqualTo("LOT-2025");
        assertThat(result.getDateExpiration()).isEqualTo(LocalDate.of(2026, 6, 30));
        verify(repository).save(ligne);
    }

    @Test
    void update_should_accept_null_lot_fields() {
        LigneCommandeAchat ligne = new LigneCommandeAchat();
        LigneCommandeAchatUpdate update = new LigneCommandeAchatUpdate(
                10,
                new BigDecimal("5.00"),
                new BigDecimal("15.00"),
                null,
                null
        );
        when(repository.save(any(LigneCommandeAchat.class))).thenAnswer(inv -> inv.getArgument(0));

        LigneCommandeAchat result = service.update(ligne, update);

        assertThat(result.getQuantite()).isEqualTo(10);
        assertThat(result.getNumeroLot()).isNull();
        assertThat(result.getDateExpiration()).isNull();
    }
}
