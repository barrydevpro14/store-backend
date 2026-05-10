package org.store.inventaire.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.inventaire.domain.model.Inventaire;

import java.util.UUID;

public interface InventaireJpaRepository extends JpaRepository<Inventaire, UUID> {
}
