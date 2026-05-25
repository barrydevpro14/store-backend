package org.store.stock.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.repository.StockRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDomainServiceTest {

    @Mock private StockRepository stockRepository;

    @InjectMocks
    private StockDomainService stockDomainService;

    private Magasin magasin;
    private Product produit;

    @BeforeEach
    void setUp() {
        magasin = new Magasin();
        magasin.setId(UUID.randomUUID());

        produit = new Product();
        produit.setId(UUID.randomUUID());
    }

    @Test
    void createOrUpdateEntry_should_initialize_stock_when_absent() {
        when(stockRepository.findByMagasinIdAndProduitId(magasin.getId(), produit.getId())).thenReturn(Optional.empty());
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        Stock stock = stockDomainService.createOrUpdateEntry(new StockEntryContext(magasin, produit, 100, new BigDecimal("10.00")));

        assertThat(stock.getMagasin()).isSameAs(magasin);
        assertThat(stock.getProduit()).isSameAs(produit);
        assertThat(stock.getQuantiteDisponible()).isEqualTo(100);
        assertThat(stock.getPrixAchatMoyen()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void createOrUpdateEntry_should_recompute_weighted_average_when_stock_exists() {
        Stock existing = new Stock();
        existing.setId(UUID.randomUUID());
        existing.setMagasin(magasin);
        existing.setProduit(produit);
        existing.setQuantiteDisponible(100);
        existing.setPrixAchatMoyen(new BigDecimal("10.00"));

        when(stockRepository.findByMagasinIdAndProduitId(magasin.getId(), produit.getId())).thenReturn(Optional.of(existing));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        Stock stock = stockDomainService.createOrUpdateEntry(new StockEntryContext(magasin, produit, 50, new BigDecimal("20.00")));

        assertThat(stock.getQuantiteDisponible()).isEqualTo(150);
        assertThat(stock.getPrixAchatMoyen()).isEqualByComparingTo(new BigDecimal("13.33"));
    }

    @Test
    void createOrUpdateEntry_should_handle_three_lots_at_different_prices() {
        Stock existing = new Stock();
        existing.setMagasin(magasin);
        existing.setProduit(produit);
        existing.setQuantiteDisponible(200);
        existing.setPrixAchatMoyen(new BigDecimal("12.50"));

        when(stockRepository.findByMagasinIdAndProduitId(magasin.getId(), produit.getId())).thenReturn(Optional.of(existing));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        Stock stock = stockDomainService.createOrUpdateEntry(new StockEntryContext(magasin, produit, 100, new BigDecimal("25.00")));

        assertThat(stock.getQuantiteDisponible()).isEqualTo(300);
        assertThat(stock.getPrixAchatMoyen()).isEqualByComparingTo(new BigDecimal("16.67"));
    }

    @Test
    void createOrUpdateEntry_should_treat_null_prixMoyen_as_zero() {
        Stock existing = new Stock();
        existing.setMagasin(magasin);
        existing.setProduit(produit);
        existing.setQuantiteDisponible(0);
        existing.setPrixAchatMoyen(null);

        when(stockRepository.findByMagasinIdAndProduitId(magasin.getId(), produit.getId())).thenReturn(Optional.of(existing));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        Stock stock = stockDomainService.createOrUpdateEntry(new StockEntryContext(magasin, produit, 10, new BigDecimal("15.00")));

        assertThat(stock.getQuantiteDisponible()).isEqualTo(10);
        assertThat(stock.getPrixAchatMoyen()).isEqualByComparingTo(new BigDecimal("15.00"));
    }
}
