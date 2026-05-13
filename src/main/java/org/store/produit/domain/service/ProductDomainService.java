package org.store.produit.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.common.model.PieceJointe;
import org.store.common.service.GlobalService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.dto.ProductResponse;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.Quality;
import org.store.produit.domain.repository.ProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductDomainService extends GlobalService<Product, ProductRepository> {
    public ProductDomainService(ProductRepository repository) {
        super(repository);
    }

    public Product create(ProductRequest productRequest, CategoryProduct categoryProduct, Quality quality, Entreprise entreprise) {
        Product product = new Product();
        product.setNom(productRequest.nom());
        product.setReference(productRequest.reference());
        product.setDescription(productRequest.description());
        product.setCategoryProduct(categoryProduct);
        product.setQuality(quality);
        product.setEntreprise(entreprise);
        return save(product);
    }

    public Page<ProductResponse> findResponsesByEntrepriseId(UUID entrepriseId, Pageable pageable) {
        return repository.findResponsesByEntrepriseId(entrepriseId, pageable);
    }

    public Optional<Product> findByReferenceAndEntrepriseId(String reference, UUID entrepriseId) {
        return repository.findByReferenceAndEntrepriseId(reference, entrepriseId);
    }

    public boolean existsByReferenceAndEntrepriseId(String reference, UUID entrepriseId) {
        return repository.existsByReferenceAndEntrepriseId(reference, entrepriseId);
    }

    public Product setImagePrincipal(Product product, PieceJointe imagePrincipal) {
        product.setImagePrincipal(imagePrincipal);
        return save(product);
    }

    public Product addImages(Product product, List<PieceJointe> images) {
        product.getImages().addAll(images);
        return save(product);
    }
}
