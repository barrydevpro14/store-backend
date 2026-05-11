package org.store.stock.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.stock.domain.model.MouvementStock;
import org.store.stock.domain.repository.MouvementStockRepository;

@Service
public class MouvementStockDomainService extends GlobalService<MouvementStock, MouvementStockRepository> {
    public MouvementStockDomainService(MouvementStockRepository repository) {
        super(repository);
    }
}
