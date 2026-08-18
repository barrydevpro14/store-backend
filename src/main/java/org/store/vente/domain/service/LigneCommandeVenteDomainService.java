package org.store.vente.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.vente.application.dto.LigneCommandeVenteCreate;
import org.store.vente.application.dto.TopProduitResponse;
import org.store.vente.application.dto.TopProduitsFilter;
import org.store.vente.domain.enums.LivraisonStatut;
import org.store.vente.domain.model.LigneCommandeVente;
import org.store.vente.domain.repository.LigneCommandeVenteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class LigneCommandeVenteDomainService extends GlobalService<LigneCommandeVente, LigneCommandeVenteRepository> {
    public LigneCommandeVenteDomainService(LigneCommandeVenteRepository repository) {
        super(repository);
    }

    /** Crée et persiste une ligne de commande vente avec montantTotal = quantite * prixUnitaire. Livraison initialisée à NON_LIVREE. */
    public LigneCommandeVente create(LigneCommandeVenteCreate ligneCommandeVenteCreate) {
        ProductFournisseur productFournisseur = ligneCommandeVenteCreate.productFournisseur();
        BigDecimal quantite = ligneCommandeVenteCreate.quantite();
        BigDecimal prixUnitaire = ligneCommandeVenteCreate.prixUnitaire();

        LigneCommandeVente ligne = new LigneCommandeVente();
        ligne.setCommande(ligneCommandeVenteCreate.commande());
        ligne.setProductFournisseur(productFournisseur);
        ligne.setProduct(productFournisseur.getProduct());
        ligne.setQuantite(quantite);
        ligne.setQuantiteLivree(BigDecimal.ZERO);
        ligne.setLivraisonStatut(LivraisonStatut.NON_LIVREE);
        ligne.setPrixUnitaire(prixUnitaire);
        ligne.setMontantTotal(prixUnitaire.multiply(quantite).setScale(2, java.math.RoundingMode.HALF_UP));
        ligne.setDateAjout(LocalDate.now());
        return save(ligne);
    }

    /** Retourne les lignes d'une commande paginées (utilisé par le formulaire de saisie brouillon). */
    public Page<LigneCommandeVente> findPagedByCommandeId(UUID commandeId, int page, int size) {
        return repository.findPagedByCommandeId(commandeId, PageRequest.of(page, size));
    }

    /** Top N produits les plus vendus (par quantité) dans le magasin sur la plage du filter, paginés. */
    public Page<TopProduitResponse> findTopProduitsForCaisse(TopProduitsFilter filter, UUID entrepriseId) {
        return repository.findTopProduitsByMagasinAndDay(filter.magasinId(), entrepriseId,
                filter.startOfDay(), filter.endOfDay(), filter.toPageable());
    }

    /** Met à jour quantité et prix unitaire d'une ligne en DRAFT (recalcule montantTotal, livraison inchangée). */
    public LigneCommandeVente update(LigneCommandeVente ligne, BigDecimal quantite, BigDecimal prixUnitaire) {
        ligne.setQuantite(quantite);
        ligne.setPrixUnitaire(prixUnitaire);
        ligne.setMontantTotal(prixUnitaire.multiply(quantite).setScale(2, java.math.RoundingMode.HALF_UP));
        return save(ligne);
    }

    /** Recalcule `livraisonStatut` depuis `quantiteLivree` et met à jour la ligne. Appelé par le use-case livraison. */
    public LigneCommandeVente applyLivraison(LigneCommandeVente ligne, BigDecimal quantiteLivree) {
        ligne.setQuantiteLivree(quantiteLivree);
        ligne.setLivraisonStatut(computeStatut(quantiteLivree, ligne.getQuantite()));
        return save(ligne);
    }

    /** Règle métier unique : `LIVREE` si tout, `NON_LIVREE` si zéro, `PARTIELLEMENT_LIVREE` sinon. */
    private LivraisonStatut computeStatut(BigDecimal quantiteLivree, BigDecimal quantite) {
        if (quantiteLivree.compareTo(BigDecimal.ZERO) == 0) return LivraisonStatut.NON_LIVREE;
        if (quantiteLivree.compareTo(quantite) == 0) return LivraisonStatut.LIVREE;
        return LivraisonStatut.PARTIELLEMENT_LIVREE;
    }
}
