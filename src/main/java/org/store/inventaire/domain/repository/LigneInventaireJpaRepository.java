package org.store.inventaire.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.inventaire.domain.model.LigneInventaire;

import java.util.UUID;

public interface LigneInventaireJpaRepository extends JpaRepository<LigneInventaire, UUID> {
}
