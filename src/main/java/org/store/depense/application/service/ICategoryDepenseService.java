package org.store.depense.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.depense.application.dto.CategoryDepenseRequest;
import org.store.depense.application.dto.CategoryDepenseResponse;
import org.store.depense.domain.model.CategoryDepense;

import java.util.UUID;

public interface ICategoryDepenseService {

    CategoryDepenseResponse create(CategoryDepenseRequest categoryDepenseRequest);

    CategoryDepense findById(UUID id);

    CategoryDepenseResponse findResponseById(UUID id);

    Page<CategoryDepenseResponse> findAllByCurrentEntreprise(Pageable pageable);

    CategoryDepenseResponse update(UUID id, CategoryDepenseRequest categoryDepenseRequest);

    void delete(UUID id);

    /** Lève ForbiddenException si la catégorie n'appartient pas à l'entreprise du caller. */
    CategoryDepense ensureBelongsToCurrentEntreprise(CategoryDepense categoryDepense);
}
