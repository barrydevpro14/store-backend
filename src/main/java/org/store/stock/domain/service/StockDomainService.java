package org.store.stock.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.repository.StockRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
public class StockDomainService extends GlobalService<Stock, StockRepository> {
    public StockDomainService(StockRepository repository) {
        super(repository);
    }

    public Optional<Stock> findByMagasinIdAndProduitId(UUID magasinId, UUID produitId) {
        return repository.findByMagasinIdAndProduitId(magasinId, produitId);
    }

    public Page<StockResponse> findResponsesByFilters(UUID entrepriseId, UUID magasinId, UUID productId, Pageable pageable) {
        return repository.findResponsesByFilters(entrepriseId, magasinId, productId, pageable);
    }

    public Stock upsertOnEntry(Magasin magasin, Product produit, int quantite, BigDecimal prixAchat) {
        Stock stock = findByMagasinIdAndProduitId(magasin.getId(), produit.getId())
                .orElseGet(() -> initStock(magasin, produit));
        int qtyAvant = stock.getQuantiteDisponible();
        int qtyApres = qtyAvant + quantite;
        BigDecimal prixMoyenAvant = stock.getPrixAchatMoyen() != null ? stock.getPrixAchatMoyen() : BigDecimal.ZERO;
        BigDecimal nouvelleMoyenne = prixMoyenAvant.multiply(BigDecimal.valueOf(qtyAvant))
                .add(prixAchat.multiply(BigDecimal.valueOf(quantite)))
                .divide(BigDecimal.valueOf(qtyApres), 2, RoundingMode.HALF_UP);
        stock.setQuantiteDisponible(qtyApres);
        stock.setPrixAchatMoyen(nouvelleMoyenne);
        return save(stock);
    }

    private Stock initStock(Magasin magasin, Product produit) {
        Stock stock = new Stock();
        stock.setMagasin(magasin);
        stock.setProduit(produit);
        stock.setQuantiteDisponible(0);
        stock.setPrixAchatMoyen(BigDecimal.ZERO);
        return stock;
    }
}
