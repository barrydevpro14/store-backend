package org.store.vente.application.service;

import java.util.UUID;

/** Generates PDF bytes for a client invoice or a quote (devis). Scoped to the current user's entreprise. */
public interface IInvoicePdfService {
    byte[] generate(UUID factureId, UUID configId);
    byte[] generateForCommande(UUID commandeId, UUID configId);
}
