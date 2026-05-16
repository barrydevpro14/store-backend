package org.store.vente.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.common.tools.ReferenceHelper;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.CommandeVenteCreate;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.repository.CommandeVenteRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class CommandeVenteDomainService extends GlobalService<CommandeVente, CommandeVenteRepository> {
    public CommandeVenteDomainService(CommandeVenteRepository repository) {
        super(repository);
    }

    /** Crée et persiste une commande de vente. Les montants vivent uniquement sur FactureClient (depuis F-V3-bis). */
    public CommandeVente create(CommandeVenteCreate commandeVenteCreate) {
        CommandeVente commande = new CommandeVente();
        commande.setClient(commandeVenteCreate.client());
        commande.setMagasin(commandeVenteCreate.magasin());
        commande.setDate(commandeVenteCreate.dateVente());
        commande.setReference(commandeVenteCreate.reference());
        commande.setStatut(commandeVenteCreate.statut());
        return save(commande);
    }

    /** Génère une référence unique au format VTE-yyyyMMdd-HHmmssSSS. */
    public String generateReference() {
        return ReferenceHelper.generate("VTE");
    }

    /** Listing paginé filtré scopé entreprise (projection JPQL, user toujours null). */
    public Page<CommandeVenteResponse> findResponsesByFilter(CommandeVenteFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(filter, entrepriseId, filter.toPageable());
    }

    /** Détails projetés JPQL avec user résolu (Account.createdBy -> Utilisateur via CAST + JOIN), scopé entreprise. */
    public Optional<CommandeVenteResponse> findResponseById(UUID id, UUID entrepriseId) {
        return repository.findResponseById(id, entrepriseId);
    }

    /** Nombre de commandes créées dans le magasin entre les bornes [startOfDay, endOfDay] du filter. */
    public long countCommandesForCaisse(CaisseResumeFilter filter, UUID entrepriseId) {
        return repository.countByMagasinAndDay(filter.magasinId(), entrepriseId, filter.startOfDay(), filter.endOfDay());
    }

    /** Somme des quantités vendues (toutes lignes) dans le magasin sur la journée du filter. */
    public long sumQuantiteProduitsForCaisse(CaisseResumeFilter filter, UUID entrepriseId) {
        return repository.sumQuantiteLignesByMagasinAndDay(filter.magasinId(), entrepriseId, filter.startOfDay(), filter.endOfDay());
    }
}
