package org.store.stock.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.stock.domain.model.MouvementStock;
import org.store.stock.domain.repository.MouvementStockJpaRepository;

@Service
public class MouvementStockDomainService extends GlobalService<MouvementStock, MouvementStockJpaRepository> {
    public MouvementStockDomainService(MouvementStockJpaRepository repository) {
        super(repository);
    }
}
