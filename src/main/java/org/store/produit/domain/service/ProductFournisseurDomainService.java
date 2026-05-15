package org.store.produit.domain.service;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.service.GlobalService;
import org.store.produit.application.dto.ProductFournisseurRequest;
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

    public ProductFournisseur create(ProductFournisseurRequest productFournisseurRequest, Product product, Fournisseur fournisseur, Quality quality) {
        ProductFournisseur productFournisseur = new ProductFournisseur();
        productFournisseur.setProduct(product);
        productFournisseur.setFournisseur(fournisseur);
        productFournisseur.setQuality(quality);
        productFournisseur.setPrixAchat(productFournisseurRequest.prixAchat());
        productFournisseur.setPrixVente(productFournisseurRequest.prixVente());
        productFournisseur.setReferenceFournisseur(productFournisseurRequest.referenceFournisseur());
        productFournisseur.setOrigine(productFournisseurRequest.origine());
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
