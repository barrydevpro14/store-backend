package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.application.dto.LigneCommandeAchatCreate;
import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.achat.domain.repository.LigneCommandeAchatRepository;
import org.store.common.service.GlobalService;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    public LigneCommandeAchat update(LigneCommandeAchat ligne, int quantite, BigDecimal prixAchat, BigDecimal prixVente,
                                     String numeroLot, LocalDate dateExpiration) {
        ligne.setQuantite(quantite);
        ligne.setPrixAchat(prixAchat);
        ligne.setPrixVente(prixVente);
        ligne.setNumeroLot(numeroLot);
        ligne.setDateExpiration(dateExpiration);
        return save(ligne);
    }

    /** Incrémente la quantité déjà reçue sur une ligne lors d'une réception partielle. */
    public LigneCommandeAchat incrementQuantiteRecue(LigneCommandeAchat ligne, int quantite) {
        ligne.setQuantiteRecue(ligne.getQuantiteRecue() + quantite);
        return save(ligne);
    }
}
