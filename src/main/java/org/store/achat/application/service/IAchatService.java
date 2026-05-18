package org.store.achat.application.service;

import org.store.achat.application.dto.AchatDetailsResponse;
import org.store.achat.application.dto.AchatDraftResponse;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.dto.AchatValidateRequest;
import org.store.achat.application.dto.LigneAchatUpdateRequest;
import org.store.achat.application.dto.LigneCommandeAchatResponse;

import java.util.UUID;

public interface IAchatService {

    /**
     * Crée une commande d'achat en statut DRAFT : commande + lignes (snapshot prix + traçabilité lot).
     * Pas de facture, pas d'entrée stock, pas de mise à jour du prixVente PF — tout est matérialisé
     * à la validation. Permet la visualisation et l'édition avant engagement.
     */
    AchatDraftResponse create(AchatRequest achatRequest);

    /**
     * Matérialise une commande DRAFT : crée la FactureAchat (montantTotal recalculé depuis les lignes
     * courantes), produit les entrées stock + journal ENTREE_ACHAT par ligne, met à jour le prixVente
     * courant des PF, et bascule le statut en RECEPTIONNEE. Lève BadArgument si la commande n'est pas en DRAFT.
     */
    AchatResponse validate(UUID commandeId, AchatValidateRequest achatValidateRequest);

    /**
     * Édite une ligne d'une commande DRAFT (quantité, prix, traçabilité lot). Garde stricte : commande
     * en DRAFT + ligne appartient à la commande ciblée. Re-valide prixVente > prixAchat.
     */
    LigneCommandeAchatResponse updateLigne(UUID commandeId, UUID ligneId, LigneAchatUpdateRequest ligneAchatUpdateRequest);

    /**
     * Supprime une ligne d'une commande DRAFT. Garde : commande en DRAFT + ligne appartient à la commande
     * + refus si dernière ligne (commande vide interdite).
     */
    void deleteLigne(UUID commandeId, UUID ligneId);

    /**
     * Détails d'un achat : commande + facture éventuelle (null si DRAFT) + lignes (produit, quantité, prix, lot).
     * Scoping entreprise du caller via la commande.
     */
    AchatDetailsResponse findDetailsById(UUID commandeId);
}
