package org.store.achat.application.service;

import org.store.achat.application.dto.AchatDetailsResponse;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;

import java.util.UUID;

public interface IAchatService {

    /**
     * Crée un achat complet de manière atomique : commande (RECEPTIONNEE),
     * lignes de commande, facture (NON_PAYEE), entrées stock pour chaque ligne,
     * upsert du stock agrégé et journalisation des mouvements ENTREE_ACHAT.
     */
    AchatResponse create(AchatRequest achatRequest);

    /**
     * Détails d'un achat : commande + facture associée + toutes ses lignes (produit, quantité, prixAchat, prixVente).
     * Scoping entreprise du caller via la commande.
     */
    AchatDetailsResponse findDetailsById(UUID commandeId);
}
