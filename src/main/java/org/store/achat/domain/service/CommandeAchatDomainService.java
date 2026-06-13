package org.store.achat.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.achat.application.dto.CommandeAchatCreate;
import org.store.achat.application.dto.CommandeAchatFilter;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.MotifAnnulationAchat;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.repository.CommandeAchatRepository;
import org.store.common.service.GlobalService;
import org.store.common.tools.ReferenceHelper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CommandeAchatDomainService extends GlobalService<CommandeAchat, CommandeAchatRepository> {

    public CommandeAchatDomainService(CommandeAchatRepository repository) {
        super(repository);
    }

    /** Crée et persiste une commande d'achat à partir d'un record groupé. */
    public CommandeAchat create(CommandeAchatCreate commandeAchatCreate) {
        CommandeAchat commande = new CommandeAchat();
        commande.setFournisseur(commandeAchatCreate.fournisseur());
        commande.setMagasin(commandeAchatCreate.magasin());
        commande.setDate(commandeAchatCreate.dateCommande());
        commande.setReference(commandeAchatCreate.reference());
        commande.setStatut(commandeAchatCreate.statut());
        return save(commande);
    }

    /** Génère une référence unique au format CMD-yyyyMMdd-HHmmssSSS. */
    public String generateReference() {
        return ReferenceHelper.generate("CMD");
    }

    public Page<CommandeAchatResponse> findResponsesByFilter(CommandeAchatFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(filter, entrepriseId, filter.toPageable());
    }

    /** Bascule la commande en statut RECEPTIONNEE quand toutes les lignes ont été totalement reçues. */
    public CommandeAchat markReceptionnee(CommandeAchat commande) {
        commande.setStatut(CommandeAchatStatut.RECEPTIONNEE);
        return save(commande);
    }

    /** Bascule la commande en statut ANNULEE en horodatant et en archivant motif + commentaire. */
    public CommandeAchat cancel(CommandeAchat commande, MotifAnnulationAchat motif, String commentaire) {
        commande.setStatut(CommandeAchatStatut.ANNULEE);
        commande.setMotifAnnulation(motif);
        commande.setCommentaireAnnulation(commentaire);
        commande.setDateAnnulation(LocalDateTime.now());
        return save(commande);
    }

    /** Compte les commandes d'achat dans un statut donné pour un magasin. */
    public long countByMagasinIdAndStatut(UUID magasinId, CommandeAchatStatut statut) {
        return repository.countByMagasinIdAndStatut(magasinId, statut);
    }

    /** Compte les commandes d'achat dans un statut donné pour toute l'entreprise. */
    public long countByEntrepriseAndStatut(UUID entrepriseId, CommandeAchatStatut statut) {
        return repository.countByEntrepriseAndStatut(entrepriseId, statut);
    }

    /** Met à jour le montant total dénormalisé de la commande. */
    public CommandeAchat updateMontantTotal(CommandeAchat commande, BigDecimal montantTotal) {
        commande.setMontantTotal(montantTotal.max(BigDecimal.ZERO));
        return save(commande);
    }

    /** Retourne true si au moins une commande achat a ete creee par ce compte (audit createdBy). */
    public boolean hasCommandesByAccount(String accountId) {
        return repository.existsByCreatedBy(accountId);
    }
}
