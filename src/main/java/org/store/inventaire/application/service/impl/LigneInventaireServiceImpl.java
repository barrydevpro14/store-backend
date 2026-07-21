package org.store.inventaire.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.inventaire.application.dto.LigneInventaireResponse;
import org.store.inventaire.application.service.ILigneInventaireService;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.model.LigneInventaire;
import org.store.inventaire.domain.service.LigneInventaireDomainService;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Délègue la gestion des lignes d'inventaire au domain service.
 */
@Service
@Transactional(readOnly = true)
public class LigneInventaireServiceImpl implements ILigneInventaireService {

    private final LigneInventaireDomainService ligneInventaireDomainService;

    public LigneInventaireServiceImpl(LigneInventaireDomainService ligneInventaireDomainService) {
        this.ligneInventaireDomainService = ligneInventaireDomainService;
    }

    @Override
    @Transactional
    public LigneInventaire create(Inventaire inventaire, ProductFournisseur productFournisseur,
                                  int quantiteTheorique, int quantiteReelle, BigDecimal prixUnitaire) {
        return ligneInventaireDomainService.create(inventaire, productFournisseur, quantiteTheorique, quantiteReelle, prixUnitaire);
    }

    @Override
    public LigneInventaire findLigne(UUID ligneId) {
        return ligneInventaireDomainService.findLigne(ligneId);
    }

    @Override
    public Optional<LigneInventaire> findByInventaireIdAndProductFournisseurId(UUID inventaireId, UUID productFournisseurId) {
        return ligneInventaireDomainService.findByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId);
    }

    @Override
    public List<LigneInventaire> findAllByInventaireId(UUID inventaireId) {
        return ligneInventaireDomainService.findAllByInventaireId(inventaireId);
    }

    @Override
    public Page<LigneInventaireResponse> findResponsesByInventaireId(UUID inventaireId, Pageable pageable) {
        return ligneInventaireDomainService.findResponsesByInventaireId(inventaireId, pageable);
    }

    @Override
    @Transactional
    public LigneInventaire updateQuantiteReelle(LigneInventaire ligne, int quantiteReelle) {
        return ligneInventaireDomainService.updateQuantiteReelle(ligne, quantiteReelle);
    }

    @Override
    @Transactional
    public LigneInventaire updateQuantiteTheorique(LigneInventaire ligne, int quantiteTheorique) {
        return ligneInventaireDomainService.updateQuantiteTheorique(ligne, quantiteTheorique);
    }

    @Override
    @Transactional
    public void delete(LigneInventaire ligne) {
        ligneInventaireDomainService.delete(ligne);
    }

    @Override
    public boolean existsByInventaireIdAndProductFournisseurId(UUID inventaireId, UUID productFournisseurId) {
        return ligneInventaireDomainService.existsByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId);
    }
}
