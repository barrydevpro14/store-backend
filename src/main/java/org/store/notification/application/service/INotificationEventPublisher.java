package org.store.notification.application.service;

import org.store.notification.application.event.ContactMessageReceivedEvent;
import org.store.notification.application.event.ContactMessageRepliedEvent;
import org.store.notification.application.event.PaiementAbonnementRejectedEvent;
import org.store.notification.application.event.PaiementAbonnementSubmittedEvent;
import org.store.notification.application.event.PaiementAbonnementValidatedEvent;
import org.store.notification.application.event.StockBelowThresholdEvent;
import org.store.notification.application.event.VenteValidatedEvent;

public interface INotificationEventPublisher {
    void publishVenteValidated(VenteValidatedEvent event);
    void publishStockBelowThreshold(StockBelowThresholdEvent event);
    void publishPaiementSubmitted(PaiementAbonnementSubmittedEvent event);
    void publishPaiementValidated(PaiementAbonnementValidatedEvent event);
    void publishPaiementRejected(PaiementAbonnementRejectedEvent event);
    void publishContactMessageReceived(ContactMessageReceivedEvent event);
    void publishContactMessageReplied(ContactMessageRepliedEvent event);

    /** Generic publish — used by AlertScheduler for alert events. */
    void publishEvent(Object event);
}
