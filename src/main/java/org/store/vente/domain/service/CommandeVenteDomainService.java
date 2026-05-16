package org.store.vente.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.common.tools.ReferenceHelper;
import org.store.vente.application.dto.CommandeVenteCreate;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.repository.CommandeVenteRepository;

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
        commande.setMontantPaye(java.math.BigDecimal.ZERO);
        return save(commande);
    }

    /** Met à jour le montant total cumulé de la commande après calcul des lignes. */
    public CommandeVente applyMontantTotal(CommandeVente commande, java.math.BigDecimal montantTotal) {
        commande.setMontantTotal(montantTotal);
        return save(commande);
    }

    /** Génère une référence unique au format VTE-yyyyMMdd-HHmmssSSS. */
    public String generateReference() {
        return ReferenceHelper.generate("VTE");
    }
}
