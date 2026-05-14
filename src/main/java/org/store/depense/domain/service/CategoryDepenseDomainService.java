package org.store.depense.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.depense.application.dto.CategoryDepenseRequest;
import org.store.depense.application.dto.CategoryDepenseResponse;
import org.store.depense.domain.model.CategoryDepense;
import org.store.depense.domain.repository.CategoryDepenseRepository;
import org.store.entreprise.domain.model.Entreprise;

import java.util.Optional;
import java.util.UUID;

@Service
public class CategoryDepenseDomainService extends GlobalService<CategoryDepense, CategoryDepenseRepository> {
    public CategoryDepenseDomainService(CategoryDepenseRepository repository) {
        super(repository);
    }

    public CategoryDepense create(CategoryDepenseRequest categoryDepenseRequest, Entreprise entreprise) {
        CategoryDepense category = new CategoryDepense();
        category.setEntreprise(entreprise);
        category.setNom(categoryDepenseRequest.nom());
        category.setDescription(categoryDepenseRequest.description());
        category.setActif(categoryDepenseRequest.actif() == null || categoryDepenseRequest.actif());
        return save(category);
    }

    public Optional<CategoryDepense> findByNomAndEntrepriseId(String nom, UUID entrepriseId) {
        return repository.findByNomAndEntrepriseId(nom, entrepriseId);
    }

    public boolean existsByNomAndEntrepriseId(String nom, UUID entrepriseId) {
        return repository.existsByNomAndEntrepriseId(nom, entrepriseId);
    }

    public Page<CategoryDepenseResponse> findResponsesByEntrepriseId(UUID entrepriseId, Pageable pageable) {
        return repository.findResponsesByEntrepriseId(entrepriseId, pageable);
    }
}
