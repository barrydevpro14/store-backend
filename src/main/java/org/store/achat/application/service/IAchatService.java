package org.store.achat.application.service;

import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;

public interface IAchatService {

    /**
     * Crée un achat complet de manière atomique : commande (RECEPTIONNEE),
     * lignes de commande, facture (NON_PAYEE), entrées stock pour chaque ligne,
     * upsert du stock agrégé et journalisation des mouvements ENTREE_ACHAT.
     */
    AchatResponse create(AchatRequest achatRequest);
}
