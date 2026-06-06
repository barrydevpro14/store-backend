package org.store.achat.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.achat.application.dto.PaiementAchatRequest;
import org.store.achat.application.dto.PaiementAchatResponse;

import java.util.UUID;

public interface IPaiementAchatService {

    /**
     * Enregistre un paiement contre une facture achat, met à jour montantPaye et le statut
     * selon le rapport montantPaye/montantTotal. Refuse si overpaiement.
     */
    PaiementAchatResponse create(UUID factureId, PaiementAchatRequest paiementAchatRequest);

    /** Liste paginée des paiements d'une facture, scopée par entreprise du caller. */
    Page<PaiementAchatResponse> findByFactureId(UUID factureId, Pageable pageable);
}
