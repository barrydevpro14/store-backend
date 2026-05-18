package org.store.vente.application.service;

import org.store.vente.application.dto.AnnulationVenteRequest;
import org.store.vente.application.dto.AnnulationVenteResponse;
import org.store.vente.application.dto.LigneCommandeVenteResponse;
import org.store.vente.application.dto.LigneVenteUpdateRequest;
import org.store.vente.application.dto.VenteDetailsResponse;
import org.store.vente.application.dto.VenteDraftResponse;
import org.store.vente.application.dto.VenteRequest;
import org.store.vente.application.dto.VenteResponse;
import org.store.vente.application.dto.VenteValidateRequest;

import java.util.UUID;

public interface IVenteService {

    /**
     * Crée une commande de vente en statut DRAFT : commande + lignes (snapshot prix unitaire).
     * Pas de consommation stock, pas de facture, pas de paiement. Le vendeur (user courant) doit
     * être un Employe (tracé via {@code createdBy}) ; les validations {@code prixUnitaire ≥ pf.prixVente}
     * et scoping PF sont appliquées dès la création.
     */
    VenteDraftResponse create(VenteRequest venteRequest);

    /**
     * Matérialise une commande DRAFT : consomme le stock FIFO par ligne (sorties + journal SORTIE_VENTE),
     * crée la FactureClient (numéro auto + {@code dateEcheance} saisie), applique un éventuel premier paiement,
     * bascule le statut → DELIVERED. Lève BadArgument si la commande n'est pas en DRAFT.
     */
    VenteResponse validate(UUID commandeId, VenteValidateRequest venteValidateRequest);

    /**
     * Édite une ligne d'une commande DRAFT (quantité, prixUnitaire). Garde stricte :
     * commande en DRAFT + ligne appartient à la commande. Re-valide {@code prixUnitaire ≥ pf.prixVente}.
     * Le {@code productFournisseur} reste immuable.
     */
    LigneCommandeVenteResponse updateLigne(UUID commandeId, UUID ligneId, LigneVenteUpdateRequest ligneVenteUpdateRequest);

    /**
     * Supprime une ligne d'une commande DRAFT. Refuse si dernière ligne (commande vide interdite).
     */
    void deleteLigne(UUID commandeId, UUID ligneId);

    /**
     * Détails d'une vente : commande + facture éventuelle (null si DRAFT) + lignes + paiements.
     * Scoping entreprise du caller via le magasin de la commande.
     */
    VenteDetailsResponse findDetailsById(UUID commandeId);

    /**
     * Annule une vente DELIVERED dans la fenêtre temporelle autorisée :
     * ré-injecte le stock FIFO consommé (crédit lots + flag annulee sur SortieStock + journalise RETOUR_CLIENT),
     * bascule commande et facture en statut ANNULEE. Les paiements existants sont conservés (audit).
     */
    AnnulationVenteResponse cancel(UUID commandeId, AnnulationVenteRequest annulationVenteRequest);
}
