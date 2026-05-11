package org.store.produit.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.repository.ProductRepository;

@Service
public class ProductDomainService extends GlobalService<Product, ProductRepository> {
    public ProductDomainService(ProductRepository repository) {
        super(repository);
    }
}
