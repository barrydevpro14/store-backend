package org.store.depense.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.depense.domain.model.CategoryDepense;
import org.store.depense.domain.repository.CategoryDepenseRepository;

import java.util.UUID;

public interface CategoryDepenseJpaRepository extends JpaRepository<CategoryDepense, UUID>, CategoryDepenseRepository {
}
