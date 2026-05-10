package org.store.depense.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.depense.domain.model.CategoryDepense;
import org.store.depense.domain.repository.CategoryDepenseJpaRepository;

@Service
public class CategoryDepenseDomainService extends GlobalService<CategoryDepense, CategoryDepenseJpaRepository> {
    public CategoryDepenseDomainService(CategoryDepenseJpaRepository repository) {
        super(repository);
    }
}
