package org.store.magasin.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.magasin.domain.model.Magasin;

import java.util.UUID;

public interface MagasinJpaRepository extends JpaRepository<Magasin, UUID> {
}
