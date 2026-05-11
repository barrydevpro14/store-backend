package org.store.stock.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.repository.StockRepository;

@Service
public class StockDomainService extends GlobalService<Stock, StockRepository> {
    public StockDomainService(StockRepository repository) {
        super(repository);
    }
}
