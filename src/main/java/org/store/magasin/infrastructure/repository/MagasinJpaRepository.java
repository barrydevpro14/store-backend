package org.store.magasin.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.magasin.domain.model.Magasin;
import org.store.magasin.domain.repository.MagasinRepository;

import java.util.UUID;

@Repository
public interface MagasinJpaRepository extends JpaRepository<Magasin, UUID>, MagasinRepository {
}
