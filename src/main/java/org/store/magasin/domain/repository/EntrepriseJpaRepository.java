package org.store.magasin.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.magasin.domain.model.Entreprise;

import java.util.UUID;

public interface EntrepriseJpaRepository extends JpaRepository<Entreprise, UUID> {
}
