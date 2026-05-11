package org.store.depense.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.depense.domain.model.Depense;
import org.store.depense.domain.repository.DepenseRepository;

import java.util.UUID;

public interface DepenseJpaRepository extends JpaRepository<Depense, UUID>, DepenseRepository {
}
