package org.store.stock.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.ExpiringLotResponse;
import org.store.stock.application.dto.ExpiringLotsFilter;
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

    public List<EntreeStock> findAvailableLotsForFifoByProductFournisseur(UUID magasinId, UUID productFournisseurId) {
        return repository.findAvailableLotsForFifoByProductFournisseur(magasinId, productFournisseurId);
    }

    public Page<ExpiringLotResponse> findExpiringLots(ExpiringLotsFilter filter, UUID entrepriseId) {
        return repository.findExpiringLots(filter, entrepriseId, filter.toPageable());
    }

    public List<EntreeStock> findActiveLotsByMagasinAndProductIds(UUID magasinId, List<UUID> productIds) {
        return repository.findActiveLotsByMagasinAndProductIds(magasinId, productIds);
    }

    /** Création d'une entrée stock manuelle (sans commande achat). */
    public EntreeStock create(EntreeStockRequest entreeStockRequest, Magasin magasin, Product produit, ProductFournisseur productFournisseur) {
        return create(new EntreeStockCreate(
                magasin, produit, productFournisseur,
                entreeStockRequest.quantite(), entreeStockRequest.prixAchat(),
                entreeStockRequest.numeroLot(), entreeStockRequest.dateExpiration(),
                null
        ));
    }

    /** Recrédite la quantité restante d'un lot lors d'une annulation de vente (compense les SortieStock annulées). */
    public EntreeStock creditQuantiteRestante(EntreeStock lot, int quantite) {
        lot.setQuantiteRestante(lot.getQuantiteRestante() + quantite);
        return save(lot);
    }

    /** Création d'une entrée stock à partir d'un record groupé (peut être liée à une CommandeAchat). */
    public EntreeStock create(EntreeStockCreate entreeStockCreate) {
        EntreeStock entreeStock = new EntreeStock();
        entreeStock.setMagasin(entreeStockCreate.magasin());
        entreeStock.setProduit(entreeStockCreate.produit());
        entreeStock.setProductFournisseur(entreeStockCreate.productFournisseur());
        entreeStock.setQuantiteInitiale(entreeStockCreate.quantite());
        entreeStock.setQuantiteRestante(entreeStockCreate.quantite());
        entreeStock.setPrixAchat(entreeStockCreate.prixAchat());
        entreeStock.setNumeroLot(entreeStockCreate.numeroLot());
        entreeStock.setDateExpiration(entreeStockCreate.dateExpiration());
        entreeStock.setCommandeAchat(entreeStockCreate.commandeAchat());
        return save(entreeStock);
    }
}
