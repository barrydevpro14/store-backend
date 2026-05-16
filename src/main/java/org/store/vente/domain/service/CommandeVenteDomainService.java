package org.store.vente.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.common.tools.ReferenceHelper;
import org.store.vente.application.dto.CommandeVenteCreate;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.repository.CommandeVenteRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class CommandeVenteDomainService extends GlobalService<CommandeVente, CommandeVenteRepository> {
    public CommandeVenteDomainService(CommandeVenteRepository repository) {
        super(repository);
    }

    /** Crée et persiste une commande de vente à partir d'un record groupé (montantPaye initial = 0). Le vendeur est tracé via AuditableEntity.createdBy. */
    public CommandeVente create(CommandeVenteCreate commandeVenteCreate) {
        CommandeVente commande = new CommandeVente();
        commande.setClient(commandeVenteCreate.client());
        commande.setMagasin(commandeVenteCreate.magasin());
        commande.setDate(commandeVenteCreate.dateVente());
        commande.setReference(commandeVenteCreate.reference());
        commande.setStatut(commandeVenteCreate.statut());
        commande.setMontantPaye(BigDecimal.ZERO);
        return save(commande);
    }

    /** Met à jour le montant total cumulé de la commande après calcul des lignes. */
    public CommandeVente applyMontantTotal(CommandeVente commande, BigDecimal montantTotal) {
        commande.setMontantTotal(montantTotal);
        return save(commande);
    }

    /** Incrémente le montant payé cumulé de la commande (depuis montantPaye actuel, jamais ecrasement direct). */
    public CommandeVente applyMontantPaye(CommandeVente commande, BigDecimal montant) {
        BigDecimal montantPayeActuel = commande.getMontantPaye() != null ? commande.getMontantPaye() : BigDecimal.ZERO;
        commande.setMontantPaye(montantPayeActuel.add(montant));
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
}
