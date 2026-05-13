package org.store.produit.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.produit.application.dto.CategoryProductRequest;
import org.store.produit.application.dto.CategoryProductResponse;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.repository.CategoryProductRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class CategoryProductDomainService extends GlobalService<CategoryProduct, CategoryProductRepository> {
    public CategoryProductDomainService(CategoryProductRepository repository) {
        super(repository);
    }

    public CategoryProduct create(CategoryProductRequest categoryProductRequest, Entreprise entreprise) {
        CategoryProduct categoryProduct = new CategoryProduct();
        categoryProduct.setLibelle(categoryProductRequest.libelle());
        categoryProduct.setDescription(categoryProductRequest.description());
        categoryProduct.setEntreprise(entreprise);
        return save(categoryProduct);
    }

    public Page<CategoryProductResponse> findResponsesByEntrepriseId(UUID entrepriseId, Pageable pageable) {
        return repository.findResponsesByEntrepriseId(entrepriseId, pageable);
    }

    public Optional<CategoryProduct> findByLibelleAndEntrepriseId(String libelle, UUID entrepriseId) {
        return repository.findByLibelleAndEntrepriseId(libelle, entrepriseId);
    }

    public boolean existsByLibelleAndEntrepriseId(String libelle, UUID entrepriseId) {
        return repository.existsByLibelleAndEntrepriseId(libelle, entrepriseId);
    }
}
