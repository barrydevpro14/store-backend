package org.store.stock.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.repository.EntreeStockJpaRepository;

@Service
public class EntreeStockDomainService extends GlobalService<EntreeStock, EntreeStockJpaRepository> {
    public EntreeStockDomainService(EntreeStockJpaRepository repository) {
        super(repository);
    }
}
