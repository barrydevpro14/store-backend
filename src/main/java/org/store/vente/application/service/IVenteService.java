package org.store.vente.application.service;

import org.store.vente.application.dto.VenteDetailsResponse;
import org.store.vente.application.dto.VenteRequest;
import org.store.vente.application.dto.VenteResponse;

import java.util.UUID;

public interface IVenteService {

    /**
     * Crée une vente complète de manière atomique : commande (DELIVERED),
     * lignes de commande, sorties stock FIFO par PF liées aux lignes, facture client
     * (NON_PAYEE), et paiement initial éventuel. Le vendeur (user) doit être un Employe.
     */
    VenteResponse create(VenteRequest venteRequest);

    /**
     * Détails d'une vente : commande + facture associée + toutes les lignes + paiements.
     * Scoping entreprise du caller via le magasin de la commande.
     */
    VenteDetailsResponse findDetailsById(UUID commandeId);
}
