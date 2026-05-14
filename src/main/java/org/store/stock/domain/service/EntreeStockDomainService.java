package org.store.stock.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.repository.EntreeStockRepository;

import java.util.List;
import java.util.UUID;

@Service
public class EntreeStockDomainService extends GlobalService<EntreeStock, EntreeStockRepository> {
    public EntreeStockDomainService(EntreeStockRepository repository) {
        super(repository);
    }

    public List<EntreeStock> findAvailableLotsForFifo(UUID magasinId, UUID productId) {
        return repository.findAvailableLotsForFifo(magasinId, productId);
    }

    public org.springframework.data.domain.Page<org.store.stock.application.dto.ExpiringLotResponse> findExpiringLots(
            org.store.stock.application.dto.ExpiringLotsFilter filter, UUID entrepriseId) {
        return repository.findExpiringLots(filter, entrepriseId, filter.toPageable());
    }

    public EntreeStock create(EntreeStockRequest entreeStockRequest, Magasin magasin, Product produit, ProductFournisseur productFournisseur) {
        EntreeStock entreeStock = new EntreeStock();
        entreeStock.setMagasin(magasin);
        entreeStock.setProduit(produit);
        entreeStock.setProductFournisseur(productFournisseur);
        entreeStock.setQuantiteInitiale(entreeStockRequest.quantite());
        entreeStock.setQuantiteRestante(entreeStockRequest.quantite());
        entreeStock.setPrixAchat(entreeStockRequest.prixAchat());
        entreeStock.setNumeroLot(entreeStockRequest.numeroLot());
        entreeStock.setDateExpiration(entreeStockRequest.dateExpiration());
        return save(entreeStock);
    }
}
