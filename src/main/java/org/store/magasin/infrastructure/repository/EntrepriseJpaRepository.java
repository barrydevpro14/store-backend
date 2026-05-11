package org.store.magasin.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.magasin.domain.model.Entreprise;
import org.store.magasin.domain.repository.EntrepriseRepository;

import java.util.UUID;

public interface EntrepriseJpaRepository extends JpaRepository<Entreprise, UUID>, EntrepriseRepository {
}
