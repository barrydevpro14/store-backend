package org.store.plateforme.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.common.dto.DataSelect;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;
import org.store.plateforme.application.dto.CategoryDepensePlateformeFilter;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.repository.CategoryDepensePlateformeRepository;

@Service
public class CategoryDepensePlateformeDomainService extends GlobalService<CategoryDepensePlateforme, CategoryDepensePlateformeRepository> {
    public CategoryDepensePlateformeDomainService(CategoryDepensePlateformeRepository repository) {
        super(repository);
    }

    public CategoryDepensePlateforme create(CategoryDepensePlateformeRequest request) {
        CategoryDepensePlateforme category = new CategoryDepensePlateforme();
        category.setNom(request.nom());
        category.setDescription(request.description());
        category.setActif(request.actif() == null || request.actif());
        return save(category);
    }

    public boolean existsByNom(String nom) {
        return repository.existsByNom(nom);
    }

    public Page<CategoryDepensePlateformeResponse> findResponses(CategoryDepensePlateformeFilter filter) {
        return repository.findResponsesByFilter(
                filter.nom(), LikePatternHelper.toLikePattern(filter.nom()),
                filter.actif(),
                filter.startDate(), filter.endDate(),
                filter.toPageable());
    }

    public Page<DataSelect> findSelectItems(String q, Pageable pageable) {
        return repository.findSelectItems(q, LikePatternHelper.toLikePattern(q), pageable);
    }
}
