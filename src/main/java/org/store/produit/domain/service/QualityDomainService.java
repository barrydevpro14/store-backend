package org.store.produit.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.produit.domain.model.Quality;
import org.store.produit.domain.repository.QualityJpaRepository;

@Service
public class QualityDomainService extends GlobalService<Quality, QualityJpaRepository> {
    public QualityDomainService(QualityJpaRepository repository) {
        super(repository);
    }
}
