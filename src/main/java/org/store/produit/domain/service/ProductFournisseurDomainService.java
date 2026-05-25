package org.store.produit.domain.service;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.service.GlobalService;
import org.store.produit.application.dto.ProductFournisseurCreate;
import org.store.produit.application.dto.ProductFournisseurResponse;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.model.Quality;
import org.store.produit.domain.repository.ProductFournisseurRepository;

import java.util.UUID;

@Service
public class ProductFournisseurDomainService extends GlobalService<ProductFournisseur, ProductFournisseurRepository> {
    public ProductFournisseurDomainService(ProductFournisseurRepository repository) {
        super(repository);
    }

    public ProductFournisseur create(ProductFournisseurCreate productFournisseurCreate) {
        ProductFournisseur productFournisseur = new ProductFournisseur();
        productFournisseur.setProduct(productFournisseurCreate.product());
        productFournisseur.setFournisseur(productFournisseurCreate.fournisseur());
        productFournisseur.setQuality(productFournisseurCreate.quality());
        productFournisseur.setPrixAchat(productFournisseurCreate.productFournisseurRequest().prixAchat());
        productFournisseur.setPrixVente(productFournisseurCreate.productFournisseurRequest().prixVente());
        productFournisseur.setReferenceFournisseur(productFournisseurCreate.productFournisseurRequest().referenceFournisseur());
        productFournisseur.setOrigine(productFournisseurCreate.productFournisseurRequest().origine());
        return save(productFournisseur);
    }

    public ProductFournisseur updatePrixVente(ProductFournisseur productFournisseur, BigDecimal prixVente) {
        productFournisseur.setPrixVente(prixVente);
        return save(productFournisseur);
    }

    public Page<ProductFournisseurResponse> findResponsesByEntrepriseId(UUID entrepriseId, Pageable pageable) {
        return repository.findResponsesByEntrepriseId(entrepriseId, pageable);
    }

    public Page<ProductFournisseurResponse> findResponsesByProductId(UUID productId, Pageable pageable) {
        return repository.findResponsesByProductId(productId, pageable);
    }

    public boolean existsByProductIdAndFournisseurIdAndQualityId(UUID productId, UUID fournisseurId, UUID qualityId) {
        return repository.existsByProductIdAndFournisseurIdAndQualityId(productId, fournisseurId, qualityId);
    }
}
