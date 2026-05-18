package org.store.vente.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.vente.application.dto.LigneCommandeVenteCreate;
import org.store.vente.application.dto.TopProduitResponse;
import org.store.vente.application.dto.TopProduitsFilter;
import org.store.vente.domain.model.LigneCommandeVente;
import org.store.vente.domain.repository.LigneCommandeVenteRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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

    /** Top N produits les plus vendus (par quantité) dans le magasin sur la journée du filter. */
    public List<TopProduitResponse> findTopProduitsForCaisse(TopProduitsFilter filter, UUID entrepriseId) {
        return repository.findTopProduitsByMagasinAndDay(filter.magasinId(), entrepriseId,
                filter.startOfDay(), filter.endOfDay(), filter.toPageable());
    }

    /** Met à jour quantité et prix unitaire d'une ligne en DRAFT (recalcule montantTotal). */
    public LigneCommandeVente update(LigneCommandeVente ligne, int quantite, BigDecimal prixUnitaire) {
        ligne.setQuantite(quantite);
        ligne.setPrixUnitaire(prixUnitaire);
        ligne.setMontantTotal(prixUnitaire.multiply(BigDecimal.valueOf(quantite)));
        return save(ligne);
    }
}
