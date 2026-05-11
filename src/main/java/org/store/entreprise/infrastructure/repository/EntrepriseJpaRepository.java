package org.store.entreprise.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.repository.EntrepriseRepository;

import java.util.UUID;

public interface EntrepriseJpaRepository extends JpaRepository<Entreprise, UUID>, EntrepriseRepository {
}
