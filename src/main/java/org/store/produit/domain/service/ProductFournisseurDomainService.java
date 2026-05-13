package org.store.produit.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.service.GlobalService;
import org.store.produit.application.dto.ProductFournisseurRequest;
import org.store.produit.application.dto.ProductFournisseurResponse;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.repository.ProductFournisseurRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProductFournisseurDomainService extends GlobalService<ProductFournisseur, ProductFournisseurRepository> {
    public ProductFournisseurDomainService(ProductFournisseurRepository repository) {
        super(repository);
    }

    public ProductFournisseur create(ProductFournisseurRequest productFournisseurRequest, Product product, Fournisseur fournisseur) {
        ProductFournisseur productFournisseur = new ProductFournisseur();
        productFournisseur.setProduct(product);
        productFournisseur.setFournisseur(fournisseur);
        productFournisseur.setPrixAchat(productFournisseurRequest.prixAchat());
        productFournisseur.setReferenceFournisseur(productFournisseurRequest.referenceFournisseur());
        productFournisseur.setOrigine(productFournisseurRequest.origine());
        return save(productFournisseur);
    }

    public Page<ProductFournisseurResponse> findResponsesByEntrepriseId(UUID entrepriseId, Pageable pageable) {
        return repository.findResponsesByEntrepriseId(entrepriseId, pageable);
    }

    public Page<ProductFournisseurResponse> findResponsesByProductId(UUID productId, Pageable pageable) {
        return repository.findResponsesByProductId(productId, pageable);
    }

    public Optional<ProductFournisseur> findByProductIdAndFournisseurId(UUID productId, UUID fournisseurId) {
        return repository.findByProductIdAndFournisseurId(productId, fournisseurId);
    }

    public boolean existsByProductIdAndFournisseurId(UUID productId, UUID fournisseurId) {
        return repository.existsByProductIdAndFournisseurId(productId, fournisseurId);
    }
}
