package org.store.stock.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.MouvementStockFilter;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.domain.model.MouvementStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.repository.MouvementStockRepository;

import java.util.UUID;

@Service
public class MouvementStockDomainService extends GlobalService<MouvementStock, MouvementStockRepository> {
    public MouvementStockDomainService(MouvementStockRepository repository) {
        super(repository);
    }

    public MouvementStock journalize(Stock stock, MouvementJournalize command) {
        MouvementStock mouvement = new MouvementStock();
        mouvement.setStock(stock);
        mouvement.setType(command.type());
        mouvement.setQuantite(command.quantite());
        mouvement.setStockAvant(command.stockAvant());
        mouvement.setStockApres(command.stockApres());
        mouvement.setReferenceDocument(command.referenceDocument());
        mouvement.setCommentaire(command.commentaire());
        return save(mouvement);
    }

    public Page<MouvementStockResponse> findResponsesByFilter(MouvementStockFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(
                entrepriseId,
                filter.magasinId(),
                filter.productName(),
                filter.productNamePattern(),
                filter.stockId(),
                filter.typeAsEnum(),
                filter.startDate(),
                filter.endDate(),
                filter.toPageable());
    }
}
