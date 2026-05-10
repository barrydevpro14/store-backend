package org.store.stock.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.stock.domain.model.SortieStock;
import org.store.stock.domain.repository.SortieStockJpaRepository;

@Service
public class SortieStockDomainService extends GlobalService<SortieStock, SortieStockJpaRepository> {
    public SortieStockDomainService(SortieStockJpaRepository repository) {
        super(repository);
    }
}
