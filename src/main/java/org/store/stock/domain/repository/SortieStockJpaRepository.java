package org.store.stock.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.stock.domain.model.SortieStock;

import java.util.UUID;

public interface SortieStockJpaRepository extends JpaRepository<SortieStock, UUID> {
}
