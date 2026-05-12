package org.store.inventaire.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.repository.InventaireRepository;

import java.util.UUID;

@Repository
public interface InventaireJpaRepository extends JpaRepository<Inventaire, UUID>, InventaireRepository {
}
