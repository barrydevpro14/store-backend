package org.store.produit.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.produit.domain.model.Quality;
import org.store.produit.domain.repository.QualityRepository;

import java.util.UUID;

@Repository
public interface QualityJpaRepository extends JpaRepository<Quality, UUID>, QualityRepository {
}
