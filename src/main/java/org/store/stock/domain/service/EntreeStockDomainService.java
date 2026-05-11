package org.store.stock.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.repository.EntreeStockRepository;

@Service
public class EntreeStockDomainService extends GlobalService<EntreeStock, EntreeStockRepository> {
    public EntreeStockDomainService(EntreeStockRepository repository) {
        super(repository);
    }
}
