package org.store.depense.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.depense.domain.model.CategoryDepense;
import org.store.depense.domain.repository.CategoryDepenseRepository;

@Service
public class CategoryDepenseDomainService extends GlobalService<CategoryDepense, CategoryDepenseRepository> {
    public CategoryDepenseDomainService(CategoryDepenseRepository repository) {
        super(repository);
    }
}
