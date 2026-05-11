package org.store.produit.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.produit.domain.model.Quality;
import org.store.produit.domain.repository.QualityRepository;

@Service
public class QualityDomainService extends GlobalService<Quality, QualityRepository> {
    public QualityDomainService(QualityRepository repository) {
        super(repository);
    }
}
