package org.store.catalogue.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.catalogue.domain.model.CatalogueProduit;
import org.store.catalogue.domain.repository.CatalogueProduitRepository;

import java.util.UUID;

@Repository
public interface CatalogueProduitJpaRepository extends JpaRepository<CatalogueProduit, UUID>, CatalogueProduitRepository {
}
