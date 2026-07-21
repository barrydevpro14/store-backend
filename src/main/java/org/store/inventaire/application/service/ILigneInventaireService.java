package org.store.inventaire.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.inventaire.application.dto.LigneInventaireResponse;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.model.LigneInventaire;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ILigneInventaireService {

    LigneInventaire create(Inventaire inventaire, ProductFournisseur productFournisseur,
                           int quantiteTheorique, int quantiteReelle, BigDecimal prixUnitaire);

    LigneInventaire findLigne(UUID ligneId);

    Optional<LigneInventaire> findByInventaireIdAndProductFournisseurId(UUID inventaireId, UUID productFournisseurId);

    List<LigneInventaire> findAllByInventaireId(UUID inventaireId);

    Page<LigneInventaireResponse> findResponsesByInventaireId(UUID inventaireId, Pageable pageable);

    LigneInventaire updateQuantiteReelle(LigneInventaire ligne, int quantiteReelle);

    void delete(LigneInventaire ligne);

    boolean existsByInventaireIdAndProductFournisseurId(UUID inventaireId, UUID productFournisseurId);
}
