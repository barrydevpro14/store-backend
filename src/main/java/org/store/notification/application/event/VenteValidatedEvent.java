package org.store.notification.application.event;

import org.store.vente.domain.model.CommandeVente;

/** Fired when a sale is validated (statut → VALIDATE). */
public record VenteValidatedEvent(CommandeVente commande) {}
