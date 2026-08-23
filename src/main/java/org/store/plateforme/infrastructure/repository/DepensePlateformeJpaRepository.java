package org.store.plateforme.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.plateforme.domain.model.DepensePlateforme;
import org.store.plateforme.domain.repository.DepensePlateformeRepository;

import java.util.UUID;

public interface DepensePlateformeJpaRepository extends JpaRepository<DepensePlateforme, UUID>, DepensePlateformeRepository {
}
