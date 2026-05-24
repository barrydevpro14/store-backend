package org.store.achat.application.service;

import org.store.achat.application.dto.AchatDetailsResponse;
import org.store.achat.application.dto.AchatDraftResponse;
import org.store.achat.application.dto.AchatReceiveRequest;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.dto.AnnulationAchatRequest;
import org.store.achat.application.dto.AnnulationAchatResponse;
import org.store.achat.application.dto.LigneAchatUpdateRequest;
import org.store.achat.application.dto.LigneCommandeAchatResponse;

import java.util.UUID;

public interface IAchatService {

    /**
     * Crée une commande d'achat en statut DRAFT : commande + lignes (snapshot prix + traçabilité lot).
     * Pas de facture, pas d'entrée stock, pas de mise à jour du prixVente PF — tout est matérialisé
     * à la réception. Permet la visualisation et l'édition avant engagement.
     */
    AchatDraftResponse create(AchatRequest achatRequest);

    /**
     * Réceptionne une commande DRAFT en une seule transaction : crée la FactureAchat (montantTotal
     * recalculé depuis les lignes courantes) + applique le paiement initial éventuel, puis matérialise
     * le stock pour chaque ligne (EntreeStock, upsert Stock agrégé, journal ENTREE_ACHAT, maj prixVente
     * PF, maj quantiteRecue). Bascule la commande en RECEPTIONNEE. Lève BadArgument si la commande
     * n'est pas en DRAFT.
     */
    AchatResponse receive(UUID commandeId, AchatReceiveRequest achatReceiveRequest);

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

    /**
     * Supprime une commande encore en DRAFT (abandon de brouillon) : commande + lignes en cascade.
     * Aucune facture, aucune entrée stock, aucun paiement à défaire (le DRAFT garantit l'absence
     * d'effets de bord). Refuse si la commande n'est plus en DRAFT.
     */
    void deleteDraft(UUID commandeId);

    /**
     * Annule une commande RECEPTIONNEE dans la fenêtre temporelle autorisée. Retire le stock alimenté par
     * cet achat (chaque lot doit être intact : aucun lot consommé par une vente), flag annulee=true sur
     * chaque EntreeStock, journalise un RETOUR_FOURNISSEUR par lot, bascule commande + facture en ANNULEE.
     * Les paiements éventuels sont conservés (remboursement hors-app).
     */
    AnnulationAchatResponse cancel(UUID commandeId, AnnulationAchatRequest annulationAchatRequest);
}
