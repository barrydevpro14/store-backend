package org.store.produit.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.produit.domain.model.UniteMesure;
import org.store.produit.domain.repository.UniteMesureRepository;

import java.util.UUID;

@Repository
public interface UniteMesureJpaRepository extends JpaRepository<UniteMesure, UUID>, UniteMesureRepository {
}
