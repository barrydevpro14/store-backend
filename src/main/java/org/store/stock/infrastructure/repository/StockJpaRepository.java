package org.store.stock.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.repository.StockRepository;

import java.util.UUID;

public interface StockJpaRepository extends JpaRepository<Stock, UUID>, StockRepository {
}
