package org.store.vente.application.service;

import org.springframework.data.domain.Page;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.domain.model.CommandeVente;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ICommandeVenteService {

    Page<CommandeVenteResponse> findAllByCurrentEntreprise(CommandeVenteFilter filter);

    CommandeVenteResponse findResponseById(UUID id);

    /** Nombre de commandes vente créées pour une entreprise sur une plage horaire (day KPI). */
    long countByEntrepriseAndDay(UUID entrepriseId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    /**
     * Bascule une commande VALIDATE en statut CLOTURE (verrouillage : plus d'ajout de ligne possible).
     * Vérifie que la commande est accessible par le caller et qu'elle est bien en VALIDATE.
     */
    void cloturerCommande(UUID commandeId);
    void cloturerCommande(CommandeVente commande);
}
