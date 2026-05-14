package org.store.stock.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.StockFilter;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.service.impl.StockServiceImpl;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.StockDomainService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock private StockDomainService stockDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private StockServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private UUID productId;
    private UUID stockId;

    private Entreprise entreprise;
    private Magasin magasin;
    private Product produit;
    private Stock stock;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        productId = UUID.randomUUID();
        stockId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setNom("Magasin Central");
        magasin.setEntreprise(entreprise);

        produit = new Product();
        produit.setId(productId);
        produit.setNom("Clou 10mm");
        produit.setReference("CL-10");
        produit.setEntreprise(entreprise);

        stock = new Stock();
        stock.setId(stockId);
        stock.setMagasin(magasin);
        stock.setProduit(produit);
        stock.setQuantiteDisponible(150);
        stock.setSeuilApprovisionnement(20);
        stock.setPrixAchatMoyen(new BigDecimal("13.33"));
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), entrepriseId, null, "owner", "PROPRIETAIRE", List.of("STOCK_READ"));
    }

    @Test
    void findResponseById_should_return_when_accessible() {
        when(stockDomainService.findById(stockId)).thenReturn(stock);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);

        StockResponse response = service.findResponseById(stockId);

        assertThat(response.id()).isEqualTo(stockId);
        assertThat(response.magasin().id()).isEqualTo(magasinId);
        assertThat(response.produit().id()).isEqualTo(productId);
        assertThat(response.quantiteDisponible()).isEqualTo(150);
        assertThat(response.prixAchatMoyen()).isEqualByComparingTo(new BigDecimal("13.33"));
    }

    @Test
    void findResponseById_should_propagate_forbidden_when_magasin_not_accessible() {
        when(stockDomainService.findById(stockId)).thenReturn(stock);
        when(magasinService.ensureAccessibleByCurrentUser(magasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.findResponseById(stockId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void list_should_validate_filter_and_delegate_with_currentUser_entrepriseId() {
        StockFilter filter = new StockFilter(magasinId, productId, 0, 10);
        Page<StockResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(stockDomainService.findResponsesByFilter(eq(filter), eq(entrepriseId))).thenReturn(page);

        Page<StockResponse> result = service.findAllByCurrentEntreprise(filter);

        verify(validatorService).validate(filter);
        verify(stockDomainService).findResponsesByFilter(eq(filter), eq(entrepriseId));
        assertThat(result.getContent()).isEmpty();
    }
}
