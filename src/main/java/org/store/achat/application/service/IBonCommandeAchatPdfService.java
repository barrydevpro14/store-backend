package org.store.achat.application.service;

import java.util.UUID;

/** Generates the PDF bytes for a purchase order. Scoped to the current user's entreprise. */
public interface IBonCommandeAchatPdfService {
    byte[] generate(UUID id);
}
