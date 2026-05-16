package org.store.inventaire.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.common.exceptions.EntityException;
import org.store.common.service.GlobalService;
import org.store.inventaire.application.dto.LigneInventaireResponse;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.model.LigneInventaire;
import org.store.inventaire.domain.repository.LigneInventaireRepository;
import org.store.produit.domain.model.ProductFournisseur;

import java.util.List;
import java.util.UUID;

@Service
public class LigneInventaireDomainService extends GlobalService<LigneInventaire, LigneInventaireRepository> {
    public LigneInventaireDomainService(LigneInventaireRepository repository) {
        super(repository);
    }

    /** Crée une ligne d'inventaire avec écart = quantiteReelle - quantiteTheorique. */
    public LigneInventaire create(Inventaire inventaire, ProductFournisseur productFournisseur,
                                  int quantiteTheorique, int quantiteReelle) {
        LigneInventaire ligne = new LigneInventaire();
        ligne.setInventaire(inventaire);
        ligne.setProductFournisseur(productFournisseur);
        ligne.setQuantiteTheorique(quantiteTheorique);
        ligne.setQuantiteReelle(quantiteReelle);
        ligne.setEcart(quantiteReelle - quantiteTheorique);
        return save(ligne);
    }

    public Page<LigneInventaireResponse> findResponsesByInventaireId(UUID inventaireId, Pageable pageable) {
        return repository.findResponsesByInventaireId(inventaireId, pageable);
    }

    public List<LigneInventaire> findAllByInventaireId(UUID inventaireId) {
        return repository.findAllByInventaireId(inventaireId);
    }

    public boolean existsByInventaireIdAndProductFournisseurId(UUID inventaireId, UUID productFournisseurId) {
        return repository.existsByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId);
    }

    /** Recherche d'une ligne par son id avec message d'erreur metier dedie. */
    public LigneInventaire findLigne(UUID ligneId) {
        return repository.findById(ligneId)
                .orElseThrow(() -> new EntityException("inventaire.ligne.notFound", ligneId));
    }

    /** Modifie la quantite reelle saisie et recalcule l'ecart (quantite_theorique reste fige). */
    public LigneInventaire updateQuantiteReelle(LigneInventaire ligne, int quantiteReelle) {
        ligne.setQuantiteReelle(quantiteReelle);
        ligne.setEcart(quantiteReelle - ligne.getQuantiteTheorique());
        return save(ligne);
    }
}
