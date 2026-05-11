package org.store.stock.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.stock.domain.model.SortieStock;
import org.store.stock.domain.repository.SortieStockRepository;

@Service
public class SortieStockDomainService extends GlobalService<SortieStock, SortieStockRepository> {
    public SortieStockDomainService(SortieStockRepository repository) {
        super(repository);
    }
}
