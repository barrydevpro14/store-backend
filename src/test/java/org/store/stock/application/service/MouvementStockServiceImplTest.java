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
import org.store.common.service.ValidatorService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.MouvementStockFilter;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.application.service.impl.MouvementStockServiceImpl;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.MouvementStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.MouvementStockDomainService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MouvementStockServiceImplTest {

    @Mock private MouvementStockDomainService mouvementStockDomainService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private MouvementStockServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private UUID stockId;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        stockId = UUID.randomUUID();
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null, "owner", null, null, "OWNER", List.of("STOCK_READ"));
    }

    @Test
    void list_should_validate_filter_and_delegate_with_currentUser_entrepriseId() {
        MouvementStockFilter filter = new MouvementStockFilter(magasinId, "clou", stockId, "ENTREE_ACHAT", null, null, 0, 10);
        Page<MouvementStockResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(mouvementStockDomainService.findResponsesByFilter(eq(filter), eq(entrepriseId))).thenReturn(page);

        Page<MouvementStockResponse> result = service.findAllByCurrentEntreprise(filter);

        verify(validatorService).validate(filter);
        verify(mouvementStockDomainService).findResponsesByFilter(eq(filter), eq(entrepriseId));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void journalize_should_delegate_to_domain_service_and_return_response() {
        Magasin magasin = new Magasin();
        magasin.setId(UUID.randomUUID());
        magasin.setNom("Magasin Test");

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setNom("Produit Test");

        ProductFournisseur pf = new ProductFournisseur();
        pf.setProduct(product);

        Stock stock = new Stock();
        stock.setId(UUID.randomUUID());
        stock.setMagasin(magasin);
        stock.setProductFournisseur(pf);

        MouvementJournalize cmd = new MouvementJournalize(MouvementStockType.ENTREE_ACHAT, 50, 100, 150, "CMD-001", null);

        MouvementStock mouvement = new MouvementStock();
        mouvement.setId(UUID.randomUUID());
        mouvement.setStock(stock);
        mouvement.setType(MouvementStockType.ENTREE_ACHAT);
        mouvement.setQuantite(50);
        mouvement.setStockAvant(100);
        mouvement.setStockApres(150);
        mouvement.setReferenceDocument("CMD-001");

        when(mouvementStockDomainService.journalize(stock, cmd)).thenReturn(mouvement);

        MouvementStockResponse result = service.journalize(stock, cmd);

        assertThat(result.detail().type()).isEqualTo(MouvementStockType.ENTREE_ACHAT);
        assertThat(result.detail().quantite()).isEqualTo(50);
        verify(mouvementStockDomainService).journalize(stock, cmd);
    }

    @Test
    void typeAsEnum_should_parse_valid_type_or_return_null() {
        MouvementStockFilter withType = new MouvementStockFilter(magasinId, null, null, "ENTREE_ACHAT", null, null, 0, 10);
        MouvementStockFilter withoutType = new MouvementStockFilter(magasinId, null, null, null, null, null, 0, 10);
        MouvementStockFilter blankType = new MouvementStockFilter(magasinId, null, null, "  ", null, null, 0, 10);

        assertThat(withType.typeAsEnum()).isEqualTo(MouvementStockType.ENTREE_ACHAT);
        assertThat(withoutType.typeAsEnum()).isNull();
        assertThat(blankType.typeAsEnum()).isNull();
    }
}
