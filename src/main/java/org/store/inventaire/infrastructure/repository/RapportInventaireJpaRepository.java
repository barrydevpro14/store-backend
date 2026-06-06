package org.store.inventaire.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.inventaire.domain.model.RapportInventaire;
import org.store.inventaire.domain.repository.RapportInventaireRepository;

import java.util.UUID;

@Repository
public interface RapportInventaireJpaRepository extends JpaRepository<RapportInventaire, UUID>, RapportInventaireRepository {
}
