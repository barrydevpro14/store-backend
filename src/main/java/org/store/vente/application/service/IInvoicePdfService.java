package org.store.vente.application.service;

import java.util.UUID;

public interface IInvoicePdfService {
    /** Generates the PDF bytes for a client invoice. Scoped to current user's entreprise. */
    byte[] generateFactureClientPdf(UUID factureId);
}
