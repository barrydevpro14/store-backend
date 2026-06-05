package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.application.dto.LigneCommandeAchatCreate;
import org.store.achat.application.dto.LigneCommandeAchatUpdate;
import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.achat.domain.repository.LigneCommandeAchatRepository;
import org.store.common.service.GlobalService;

@Service
public class LigneCommandeAchatDomainService extends GlobalService<LigneCommandeAchat, LigneCommandeAchatRepository> {
    public LigneCommandeAchatDomainService(LigneCommandeAchatRepository repository) {
        super(repository);
    }

    /** Crée et persiste une ligne de commande d'achat à partir d'un record groupé (snapshot prix achat + prix vente + traçabilité lot). */
    public LigneCommandeAchat create(LigneCommandeAchatCreate ligneCommandeAchatCreate) {
        LigneCommandeAchat ligne = new LigneCommandeAchat();
        ligne.setCommande(ligneCommandeAchatCreate.commande());
        ligne.setProductFournisseur(ligneCommandeAchatCreate.productFournisseur());
        ligne.setQuantite(ligneCommandeAchatCreate.quantite());
        ligne.setPrixAchat(ligneCommandeAchatCreate.prixAchat());
        ligne.setPrixVente(ligneCommandeAchatCreate.prixVente());
        ligne.setNumeroLot(ligneCommandeAchatCreate.numeroLot());
        ligne.setDateExpiration(ligneCommandeAchatCreate.dateExpiration());
        return save(ligne);
    }

    /** Met à jour quantité + prix + traçabilité lot d'une ligne en DRAFT (snapshot avant matérialisation). */
    public LigneCommandeAchat update(LigneCommandeAchat ligne, LigneCommandeAchatUpdate ligneUpdate) {
        ligne.setQuantite(ligneUpdate.quantite());
        ligne.setPrixAchat(ligneUpdate.prixAchat());
        ligne.setPrixVente(ligneUpdate.prixVente());
        ligne.setNumeroLot(ligneUpdate.numeroLot());
        ligne.setDateExpiration(ligneUpdate.dateExpiration());
        return save(ligne);
    }

}
