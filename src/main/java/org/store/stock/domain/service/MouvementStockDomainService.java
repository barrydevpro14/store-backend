package org.store.stock.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.MouvementStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.repository.MouvementStockRepository;

@Service
public class MouvementStockDomainService extends GlobalService<MouvementStock, MouvementStockRepository> {
    public MouvementStockDomainService(MouvementStockRepository repository) {
        super(repository);
    }

    public MouvementStock journalize(Stock stock, MouvementStockType type, int quantite, int stockAvant, int stockApres, String referenceDocument, String commentaire) {
        MouvementStock mouvement = new MouvementStock();
        mouvement.setStock(stock);
        mouvement.setType(type);
        mouvement.setQuantite(quantite);
        mouvement.setStockAvant(stockAvant);
        mouvement.setStockApres(stockApres);
        mouvement.setReferenceDocument(referenceDocument);
        mouvement.setCommentaire(commentaire);
        return save(mouvement);
    }
}
