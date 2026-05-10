package org.store.produit.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.repository.ProductFournisseurJpaRepository;

@Service
public class ProductFournisseurDomainService extends GlobalService<ProductFournisseur, ProductFournisseurJpaRepository> {
    public ProductFournisseurDomainService(ProductFournisseurJpaRepository repository) {
        super(repository);
    }
}
