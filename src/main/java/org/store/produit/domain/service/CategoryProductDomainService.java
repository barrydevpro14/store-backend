package org.store.produit.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.repository.CategoryProductRepository;

@Service
public class CategoryProductDomainService extends GlobalService<CategoryProduct, CategoryProductRepository> {
    public CategoryProductDomainService(CategoryProductRepository repository) {
        super(repository);
    }
}
