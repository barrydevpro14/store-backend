package org.store.plateforme.application.service;

import org.springframework.data.domain.Page;
import org.store.common.dto.DataSelect;
import org.store.plateforme.application.dto.CategoryDepensePlateformeFilter;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;

import java.util.UUID;

public interface ICategoryDepensePlateformeService {

    CategoryDepensePlateformeResponse create(CategoryDepensePlateformeRequest request);

    CategoryDepensePlateforme findById(UUID id);

    CategoryDepensePlateformeResponse findResponseById(UUID id);

    Page<CategoryDepensePlateformeResponse> findAll(CategoryDepensePlateformeFilter filter);

    Page<DataSelect> findSelectItems(String q, int page, int size);

    CategoryDepensePlateformeResponse update(UUID id, CategoryDepensePlateformeRequest request);

    void delete(UUID id);
}
