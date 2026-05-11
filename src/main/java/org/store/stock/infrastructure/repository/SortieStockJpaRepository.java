package org.store.stock.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.stock.domain.model.SortieStock;
import org.store.stock.domain.repository.SortieStockRepository;

import java.util.UUID;

public interface SortieStockJpaRepository extends JpaRepository<SortieStock, UUID>, SortieStockRepository {
}
