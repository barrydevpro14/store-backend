package org.store.stock.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.stock.domain.model.MouvementStock;
import org.store.stock.domain.repository.MouvementStockRepository;

import java.util.UUID;

@Repository
public interface MouvementStockJpaRepository extends JpaRepository<MouvementStock, UUID>, MouvementStockRepository {
}
