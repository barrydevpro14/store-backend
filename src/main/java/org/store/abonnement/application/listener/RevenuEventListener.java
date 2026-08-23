package org.store.abonnement.application.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.store.abonnement.application.dto.RevenuRecordCommand;
import org.store.abonnement.application.service.IRevenuService;
import org.store.notification.application.event.RevenuRecordedEvent;

/** Subscribes to RevenuRecordedEvent (fired from PaiementAbonnementServiceImpl.validate()) and persists the Revenu row. */
@Component
public class RevenuEventListener {

    private final IRevenuService revenuService;

    public RevenuEventListener(IRevenuService revenuService) {
        this.revenuService = revenuService;
    }

    @Async
    @EventListener
    public void onRevenuRecorded(RevenuRecordedEvent event) {
        revenuService.record(new RevenuRecordCommand(event.entrepriseId(), event.countryId(), event.datePaiement(), event.montant()));
    }
}
