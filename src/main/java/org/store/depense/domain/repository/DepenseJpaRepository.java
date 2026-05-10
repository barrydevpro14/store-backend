package org.store.depense.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.depense.domain.model.Depense;

import java.util.UUID;

public interface DepenseJpaRepository extends JpaRepository<Depense, UUID> {
}
