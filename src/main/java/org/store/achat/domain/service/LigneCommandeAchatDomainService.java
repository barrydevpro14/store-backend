package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.application.dto.LigneCommandeAchatCreate;
import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.achat.domain.repository.LigneCommandeAchatRepository;
import org.store.common.service.GlobalService;

@Service
public class LigneCommandeAchatDomainService extends GlobalService<LigneCommandeAchat, LigneCommandeAchatRepository> {
    public LigneCommandeAchatDomainService(LigneCommandeAchatRepository repository) {
        super(repository);
    }

    /** Crée et persiste une ligne de commande d'achat à partir d'un record groupé. */
    public LigneCommandeAchat create(LigneCommandeAchatCreate ligneCommandeAchatCreate) {
        LigneCommandeAchat ligne = new LigneCommandeAchat();
        ligne.setCommande(ligneCommandeAchatCreate.commande());
        ligne.setProductFournisseur(ligneCommandeAchatCreate.productFournisseur());
        ligne.setQuantite(ligneCommandeAchatCreate.quantite());
        ligne.setPrixAchat(ligneCommandeAchatCreate.prixAchat());
        return save(ligne);
    }
}
