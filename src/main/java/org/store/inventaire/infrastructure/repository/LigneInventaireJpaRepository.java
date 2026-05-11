package org.store.inventaire.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.inventaire.domain.model.LigneInventaire;
import org.store.inventaire.domain.repository.LigneInventaireRepository;

import java.util.UUID;

public interface LigneInventaireJpaRepository extends JpaRepository<LigneInventaire, UUID>, LigneInventaireRepository {
}
