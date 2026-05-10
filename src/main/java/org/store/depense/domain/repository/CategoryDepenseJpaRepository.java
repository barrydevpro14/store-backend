package org.store.depense.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.depense.domain.model.CategoryDepense;

import java.util.UUID;

public interface CategoryDepenseJpaRepository extends JpaRepository<CategoryDepense, UUID> {
}
