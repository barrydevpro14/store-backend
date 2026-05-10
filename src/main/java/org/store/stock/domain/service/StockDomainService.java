package org.store.stock.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.repository.StockJpaRepository;

@Service
public class StockDomainService extends GlobalService<Stock, StockJpaRepository> {
    public StockDomainService(StockJpaRepository repository) {
        super(repository);
    }
}
