package org.store.stock.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.stock.domain.model.MouvementStock;

import java.util.UUID;

public interface MouvementStockJpaRepository extends JpaRepository<MouvementStock, UUID> {
}
