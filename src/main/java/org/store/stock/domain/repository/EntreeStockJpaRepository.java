package org.store.stock.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.stock.domain.model.EntreeStock;

import java.util.UUID;

public interface EntreeStockJpaRepository extends JpaRepository<EntreeStock, UUID> {
}
