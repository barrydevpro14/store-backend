package org.store.stock.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.stock.domain.model.Stock;

import java.util.UUID;

public interface StockJpaRepository extends JpaRepository<Stock, UUID> {
}
