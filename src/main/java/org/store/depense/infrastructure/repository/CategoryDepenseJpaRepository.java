package org.store.depense.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.depense.domain.model.CategoryDepense;
import org.store.depense.domain.repository.CategoryDepenseRepository;

import java.util.UUID;

@Repository
public interface CategoryDepenseJpaRepository extends JpaRepository<CategoryDepense, UUID>, CategoryDepenseRepository {
}
