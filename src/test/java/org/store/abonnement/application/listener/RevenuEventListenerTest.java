package org.store.abonnement.application.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.dto.RevenuRecordCommand;
import org.store.abonnement.application.service.IRevenuService;
import org.store.notification.application.event.RevenuRecordedEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RevenuEventListenerTest {

    @Mock private IRevenuService revenuService;
    @InjectMocks private RevenuEventListener listener;

    @Test
    void onRevenuRecorded_should_delegate_to_revenuService_record() {
        UUID entrepriseId = UUID.randomUUID();
        UUID countryId = UUID.randomUUID();
        LocalDate datePaiement = LocalDate.of(2026, 8, 15);
        BigDecimal montant = new BigDecimal("15000.00");

        listener.onRevenuRecorded(new RevenuRecordedEvent(entrepriseId, countryId, datePaiement, montant));

        verify(revenuService).record(new RevenuRecordCommand(entrepriseId, countryId, datePaiement, montant));
    }
}
