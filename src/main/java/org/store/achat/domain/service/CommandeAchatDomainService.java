package org.store.achat.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.achat.application.dto.CommandeAchatCreate;
import org.store.achat.application.dto.CommandeAchatFilter;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.repository.CommandeAchatRepository;
import org.store.common.service.GlobalService;
import org.store.common.tools.ReferenceHelper;

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
}
