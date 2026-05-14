package org.store.stock.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.stock.domain.model.Stock;

import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends BaseRepository<Stock> {

    Optional<Stock> findByMagasinIdAndProduitId(UUID magasinId, UUID produitId);
}
