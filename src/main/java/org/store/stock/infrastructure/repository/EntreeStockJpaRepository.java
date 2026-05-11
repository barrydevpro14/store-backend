package org.store.stock.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.repository.EntreeStockRepository;

import java.util.UUID;

public interface EntreeStockJpaRepository extends JpaRepository<EntreeStock, UUID>, EntreeStockRepository {
}
