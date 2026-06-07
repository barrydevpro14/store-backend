package org.store.notification.application.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.store.notification.application.event.ContactMessageReceivedEvent;
import org.store.notification.application.event.ContactMessageRepliedEvent;
import org.store.notification.application.event.PaiementAbonnementRejectedEvent;
import org.store.notification.application.event.PaiementAbonnementSubmittedEvent;
import org.store.notification.application.event.PaiementAbonnementValidatedEvent;
import org.store.notification.application.event.StockBelowThresholdEvent;
import org.store.notification.application.event.VenteValidatedEvent;
import org.store.notification.application.service.INotificationEventPublisher;

/**
 * Delegates all business event publishing to the Spring ApplicationEventPublisher.
 * Each method fires a typed event record consumed asynchronously by NotificationEventListener.
 */
@Service
public class NotificationEventPublisher implements INotificationEventPublisher {

    private final ApplicationEventPublisher publisher;

    public NotificationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishVenteValidated(VenteValidatedEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishStockBelowThreshold(StockBelowThresholdEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishPaiementSubmitted(PaiementAbonnementSubmittedEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishPaiementValidated(PaiementAbonnementValidatedEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishPaiementRejected(PaiementAbonnementRejectedEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishContactMessageReceived(ContactMessageReceivedEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishContactMessageReplied(ContactMessageRepliedEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishEvent(Object event) {
        publisher.publishEvent(event);
    }
}
