package org.store.common.service;

import java.util.UUID;

/** Common contract for PDF generation endpoints. Implementations scope access to the current user's entreprise. */
public interface IPdfService {
    byte[] generate(UUID id);
}
