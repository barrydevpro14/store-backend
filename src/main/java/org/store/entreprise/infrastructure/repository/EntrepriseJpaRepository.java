package org.store.entreprise.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.repository.EntrepriseRepository;

import java.util.UUID;

@Repository
public interface EntrepriseJpaRepository extends JpaRepository<Entreprise, UUID>, EntrepriseRepository {
}
