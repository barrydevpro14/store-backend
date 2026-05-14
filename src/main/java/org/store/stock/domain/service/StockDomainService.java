package org.store.stock.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.stock.application.dto.StockFilter;
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

    public Page<StockResponse> findResponsesByFilter(StockFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(filter, entrepriseId, filter.toPageable());
    }

    public Page<StockResponse> findResponsesBelowThreshold(StockFilter filter, UUID entrepriseId) {
        return repository.findResponsesBelowThreshold(filter, entrepriseId, filter.toPageable());
    }

    public org.store.stock.application.dto.StockValuationResponse computeValuation(UUID magasinId, UUID entrepriseId) {
        return repository.computeValuation(entrepriseId, magasinId);
    }

    /** Met à jour le seuil d'approvisionnement et persiste. */
    public Stock updateThreshold(Stock stock, int seuilApprovisionnement) {
        stock.setSeuilApprovisionnement(seuilApprovisionnement);
        return save(stock);
    }

    /**
     * Décrémente la quantité disponible du stock après une sortie (ne touche pas au prix d'achat moyen).
     */
    public Stock decrement(Stock stock, int quantite) {
        stock.setQuantiteDisponible(stock.getQuantiteDisponible() - quantite);
        return save(stock);
    }

    /**
     * Crée ou met à jour le stock agrégé d'un (magasin, produit) lors d'une entrée :
     * incrémente la quantité disponible et recalcule le prix d'achat moyen pondéré
     * via la formule {@code (qtyAvant × prixMoyenAvant + quantite × prixAchat) / qtyApres}
     * (scale 2, arrondi HALF_UP). Si aucun stock n'existe pour la paire, il est initialisé à zéro avant le calcul.
     */
    public Stock createOrUpdateEntry(Magasin magasin, Product produit, int quantite, BigDecimal prixAchat) {
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
