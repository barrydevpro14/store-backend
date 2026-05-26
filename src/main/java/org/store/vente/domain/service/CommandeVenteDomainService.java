package org.store.vente.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.common.tools.ReferenceHelper;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.CommandeVenteCreate;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.dto.VenteParVendeurResponse;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.enums.MotifAnnulationVente;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.repository.CommandeVenteRepository;

import java.time.LocalDateTime;
import java.util.List;
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

    /** Nombre de commandes créées dans le magasin entre les bornes [startOfPeriod, endOfPeriod] du range. */
    public long countCommandesForCaisse(CaisseResumeFilter range, UUID entrepriseId) {
        return repository.countByMagasinAndDay(range.magasinId(), entrepriseId, range.startOfPeriod(), range.endOfPeriod());
    }

    /** Somme des quantités vendues (toutes lignes) dans le magasin sur la période du range. */
    public long sumQuantiteProduitsForCaisse(CaisseResumeFilter range, UUID entrepriseId) {
        return repository.sumQuantiteLignesByMagasinAndDay(range.magasinId(), entrepriseId, range.startOfPeriod(), range.endOfPeriod());
    }

    /** Ventilation des commandes par vendeur (Account.createdBy -> Utilisateur via CAST + JOIN), agrégée par UUID utilisateur. */
    public List<VenteParVendeurResponse> ventilationParVendeurForCaisse(CaisseResumeFilter range, UUID entrepriseId) {
        return repository.ventilationParVendeurByMagasinAndDay(range.magasinId(), entrepriseId, range.startOfPeriod(), range.endOfPeriod());
    }

    /** Nombre de commandes VALIDATE créées aujourd'hui pour toute l'entreprise (toutes magasins). */
    public long countByEntrepriseAndDay(UUID entrepriseId, java.time.LocalDateTime startOfDay, java.time.LocalDateTime endOfDay) {
        return repository.countByEntrepriseAndDay(entrepriseId, startOfDay, endOfDay);
    }

    /** Bascule la commande en statut CANCEL avec motif, commentaire et timestamp d'annulation. */
    public CommandeVente cancel(CommandeVente commande, MotifAnnulationVente motif, String commentaire) {
        commande.setStatut(CommandeVenteStatut.CANCEL);
        commande.setMotifAnnulation(motif);
        commande.setCommentaireAnnulation(commentaire);
        commande.setDateAnnulation(LocalDateTime.now());
        return save(commande);
    }

    /** Bascule la commande en statut VALIDATE lors de la validation (matérialisation stock + facture). */
    public CommandeVente validate(CommandeVente commande) {
        commande.setStatut(CommandeVenteStatut.VALIDATE);
        return save(commande);
    }
}
