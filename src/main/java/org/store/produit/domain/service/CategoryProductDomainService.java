package org.store.produit.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.repository.CategoryProductJpaRepository;

@Service
public class CategoryProductDomainService extends GlobalService<CategoryProduct, CategoryProductJpaRepository> {
    public CategoryProductDomainService(CategoryProductJpaRepository repository) {
        super(repository);
    }
}
