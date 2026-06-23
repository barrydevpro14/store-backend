package org.store.vente.application.service;

import java.util.UUID;

/** Generates the PDF bytes for a client invoice. Scoped to the current user's entreprise. */
public interface IInvoicePdfService {
    byte[] generate(UUID id);
}
