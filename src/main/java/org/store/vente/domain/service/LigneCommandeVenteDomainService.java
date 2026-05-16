package org.store.vente.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.vente.application.dto.LigneCommandeVenteCreate;
import org.store.vente.domain.model.LigneCommandeVente;
import org.store.vente.domain.repository.LigneCommandeVenteRepository;

import java.math.BigDecimal;

@Service
public class LigneCommandeVenteDomainService extends GlobalService<LigneCommandeVente, LigneCommandeVenteRepository> {
    public LigneCommandeVenteDomainService(LigneCommandeVenteRepository repository) {
        super(repository);
    }

    /** Crée et persiste une ligne de commande vente avec montantTotal = quantite * prixUnitaire. */
    public LigneCommandeVente create(LigneCommandeVenteCreate ligneCommandeVenteCreate) {
        ProductFournisseur productFournisseur = ligneCommandeVenteCreate.productFournisseur();
        int quantite = ligneCommandeVenteCreate.quantite();
        BigDecimal prixUnitaire = ligneCommandeVenteCreate.prixUnitaire();

        LigneCommandeVente ligne = new LigneCommandeVente();
        ligne.setCommande(ligneCommandeVenteCreate.commande());
        ligne.setProductFournisseur(productFournisseur);
        ligne.setProduct(productFournisseur.getProduct());
        ligne.setQuantite(quantite);
        ligne.setPrixUnitaire(prixUnitaire);
        ligne.setMontantTotal(prixUnitaire.multiply(BigDecimal.valueOf(quantite)));
        return save(ligne);
    }
}
