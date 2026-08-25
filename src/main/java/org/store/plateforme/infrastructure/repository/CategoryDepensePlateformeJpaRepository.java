package org.store.plateforme.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.repository.CategoryDepensePlateformeRepository;

import java.util.UUID;

public interface CategoryDepensePlateformeJpaRepository extends JpaRepository<CategoryDepensePlateforme, UUID>, CategoryDepensePlateformeRepository {
}
