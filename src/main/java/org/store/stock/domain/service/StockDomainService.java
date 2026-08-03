package org.store.stock.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.stock.application.dto.BelowThresholdFilter;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.application.dto.StockFilter;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.repository.StockRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class StockDomainService extends GlobalService<Stock, StockRepository> {
    public StockDomainService(StockRepository repository) {
        super(repository);
    }

    public Optional<Stock> findByMagasinIdAndProductFournisseurId(UUID magasinId, UUID productFournisseurId) {
        return repository.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId);
    }

    public Page<StockResponse> findResponsesByFilter(StockFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(
                entrepriseId,
                filter.magasinId(),
                filter.productName(),
                filter.productNamePattern(),
                filter.startDate(),
                filter.endDate(),
                filter.toPageable());
    }

    public Page<StockResponse> findResponsesBelowThreshold(BelowThresholdFilter filter, UUID entrepriseId) {
        return repository.findResponsesBelowThreshold(entrepriseId, filter.magasinId(), filter.toPageable());
    }

    public StockValuationResponse computeValuation(UUID magasinId, UUID entrepriseId) {
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

    /** Nombre de produits sous seuil pour toute l'entreprise (tous magasins confondus). */
    public long countBelowThresholdByEntreprise(UUID entrepriseId) {
        return repository.countBelowThresholdByEntreprise(entrepriseId);
    }

    /**
     * Recrédite la quantité disponible du stock (ne touche pas au prix d'achat moyen).
     * Utilisé lors d'une annulation de vente pour compenser un {@code decrement} antérieur.
     */
    public Stock creditQuantite(Stock stock, int quantite) {
        stock.setQuantiteDisponible(stock.getQuantiteDisponible() + quantite);
        return save(stock);
    }

    /**
     * Crée ou met à jour le stock agrégé d'un (magasin, produit) lors d'une entrée :
     * incrémente la quantité disponible et recalcule le prix d'achat moyen pondéré
     * via la formule {@code (qtyAvant × prixMoyenAvant + quantite × prixAchat) / qtyApres}
     * (scale 6, arrondi HALF_UP). L'arrondi à 2 décimales est appliqué uniquement à l'affichage dans {@link org.store.stock.application.dto.StockResponse}.
     * Si aucun stock n'existe pour la paire, il est initialisé à zéro avant le calcul.
     */
    public Stock createOrUpdateEntry(StockEntryContext context) {
        Stock stock = findByMagasinIdAndProductFournisseurId(context.magasin().getId(), context.productFournisseur().getId())
                .orElseGet(() -> {
                    Stock s = new Stock();
                    s.setMagasin(context.magasin());
                    s.setProductFournisseur(context.productFournisseur());
                    s.setQuantiteDisponible(0);
                    s.setPrixAchatMoyen(BigDecimal.ZERO);
                    return s;
                });

        int qtyAvant = stock.getQuantiteDisponible();
        int qtyApres = qtyAvant + context.quantite();

        BigDecimal prixMoyenAvant = stock.getPrixAchatMoyen() != null ? stock.getPrixAchatMoyen() : BigDecimal.ZERO;
        BigDecimal nouvelleMoyenne = prixMoyenAvant.multiply(BigDecimal.valueOf(qtyAvant))
                .add(context.prixAchat().multiply(BigDecimal.valueOf(context.quantite())))
                .divide(BigDecimal.valueOf(qtyApres), 6, RoundingMode.HALF_UP);

        stock.setQuantiteDisponible(qtyApres);
        stock.setPrixAchatMoyen(nouvelleMoyenne);

        return save(stock);
    }

    /**
     * Recalcule {@code quantiteDisponible} et {@code prixAchatMoyen} depuis la liste des lots actifs fournie.
     * PMP = SUM(quantiteRestante × prixAchat) / SUM(quantiteRestante). Si aucun lot actif, PMP = 0.
     */
    public Stock recalculateFromLots(Stock stock, List<EntreeStock> activeLots) {
        int totalQty = activeLots.stream().mapToInt(EntreeStock::getQuantiteRestante).sum();

        BigDecimal newPmp = totalQty > 0
                ? activeLots.stream()
                        .map(l -> l.getPrixAchat().multiply(BigDecimal.valueOf(l.getQuantiteRestante())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(totalQty), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        stock.setQuantiteDisponible(totalQty);
        stock.setPrixAchatMoyen(newPmp);
        return save(stock);
    }

    /** Retourne le stock existant pour la paire magasin+PF, ou en crée un vierge (qty=0, pmp=0) persisté. */
    public Stock findOrCreate(Magasin magasin, ProductFournisseur productFournisseur) {
        return findByMagasinIdAndProductFournisseurId(magasin.getId(), productFournisseur.getId())
                .orElseGet(() -> {
                    Stock stock = new Stock();
                    stock.setMagasin(magasin);
                    stock.setProductFournisseur(productFournisseur);
                    stock.setQuantiteDisponible(0);
                    stock.setPrixAchatMoyen(BigDecimal.ZERO);
                    return save(stock);
                });
    }

    /** Compte les produits en dessous du seuil d'approvisionnement pour un magasin. */
    public long countBelowThreshold(UUID magasinId) {
        return repository.countBelowThreshold(magasinId);
    }
}
