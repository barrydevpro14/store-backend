package org.store.produit.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.produit.domain.model.Quality;

import java.util.UUID;

public interface QualityJpaRepository extends JpaRepository<Quality, UUID> {
}
