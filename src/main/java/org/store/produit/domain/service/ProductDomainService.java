package org.store.produit.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.common.exceptions.EntityException;
import org.store.common.model.PieceJointe;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;
import org.store.common.tools.LikePatternHelper;
import org.store.entreprise.domain.model.Entreprise;
import org.store.produit.application.dto.ProductFilter;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.dto.ProductResponse;
import org.store.produit.application.dto.ProductSearchResponse;
import org.store.produit.application.dto.ProductSelectorResponse;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.repository.ProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductDomainService extends GlobalService<Product, ProductRepository> {
    public ProductDomainService(ProductRepository repository) {
        super(repository);
    }

    public Product create(ProductRequest productRequest, CategoryProduct categoryProduct, Entreprise entreprise) {
        Product product = new Product();
        product.setNom(productRequest.nom());
        product.setReference(productRequest.reference());
        product.setDescription(productRequest.description());
        product.setCategoryProduct(categoryProduct);
        product.setEntreprise(entreprise);
        return save(product);
    }

    public Page<ProductResponse> findResponsesByFilter(ProductFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(entrepriseId, filter.nom(), LikePatternHelper.toLikePattern(filter.nom()), filter.reference(), LikePatternHelper.toLikePattern(filter.reference()), filter.startDate(), filter.endDate(), filter.toPageable());
    }

    public Optional<Product> findByReferenceAndNomAndEntrepriseId(String reference, String nom, UUID entrepriseId) {
        return repository.findByReferenceAndNomAndEntrepriseId(reference, nom, entrepriseId);
    }

    public boolean existsByReferenceAndNomAndEntrepriseId(String reference, String nom, UUID entrepriseId) {
        return repository.existsByReferenceAndNomAndEntrepriseId(reference, nom, entrepriseId);
    }

    public Page<Product> searchByEntrepriseWithActiveLots(String searchTerm, UUID magasinId, UUID entrepriseId, Pageable pageable) {
        return repository.searchByEntrepriseWithActiveLots(
                LikePatternHelper.toLikePattern(searchTerm),
                magasinId,
                entrepriseId,
                pageable);
    }

    public Page<ProductSelectorResponse> searchResponsesByEntreprise(String searchTerm, UUID entrepriseId, Pageable pageable) {
        return repository.searchResponsesByEntreprise(
                LikePatternHelper.toLikePattern(searchTerm),
                entrepriseId,
                pageable);
    }

    public Product setImagePrincipal(Product product, PieceJointe imagePrincipal) {
        product.setImagePrincipal(imagePrincipal);
        return save(product);
    }

    public Product addImages(Product product, List<PieceJointe> images) {
        if (product.getImages().size() + images.size() > 2) {
            throw new EntityException("product.image.maxReached");
        }
        product.getImages().addAll(images);
        return save(product);
    }

    public Optional<PieceJointe> findImageInProduct(Product product, UUID imageId) {
        return product.getImages().stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst();
    }

    public void removeImage(Product product, PieceJointe image) {
        product.getImages().remove(image);
        save(product);
    }
}
