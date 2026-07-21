package org.store.inventaire.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.entreprise.domain.model.Entreprise;
import org.store.inventaire.application.dto.LigneInventaireResponse;
import org.store.inventaire.application.service.impl.LigneInventaireServiceImpl;
import org.store.inventaire.domain.enums.InventaireStatut;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.model.LigneInventaire;
import org.store.inventaire.domain.service.LigneInventaireDomainService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LigneInventaireServiceImplTest {

    @Mock private LigneInventaireDomainService ligneInventaireDomainService;

    @InjectMocks
    private LigneInventaireServiceImpl service;

    private UUID inventaireId;
    private UUID productFournisseurId;
    private UUID ligneId;
    private Inventaire inventaire;
    private ProductFournisseur productFournisseur;

    @BeforeEach
    void setUp() {
        inventaireId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
        ligneId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(UUID.randomUUID());

        Magasin magasin = new Magasin();
        magasin.setId(UUID.randomUUID());
        magasin.setEntreprise(entreprise);

        inventaire = new Inventaire();
        inventaire.setId(inventaireId);
        inventaire.setMagasin(magasin);
        inventaire.setStatut(InventaireStatut.EN_COURS);
        inventaire.setDate(LocalDate.now());

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setEntreprise(entreprise);

        productFournisseur = new ProductFournisseur();
        productFournisseur.setId(productFournisseurId);
        productFournisseur.setProduct(product);
        productFournisseur.setPrixAchat(new BigDecimal("10.00"));
    }

    private LigneInventaire buildLigne(int qteTheorique, int qteReelle) {
        LigneInventaire ligne = new LigneInventaire();
        ligne.setId(ligneId);
        ligne.setInventaire(inventaire);
        ligne.setProductFournisseur(productFournisseur);
        ligne.setQuantiteTheorique(qteTheorique);
        ligne.setQuantiteReelle(qteReelle);
        ligne.setEcart(qteReelle - qteTheorique);
        ligne.setPrixUnitaire(productFournisseur.getPrixAchat());
        return ligne;
    }

    @Test
    void create_should_delegate_to_domain_service() {
        LigneInventaire created = buildLigne(10, 8);
        when(ligneInventaireDomainService.create(inventaire, productFournisseur, 10, 8, new BigDecimal("10.00")))
                .thenReturn(created);

        LigneInventaire result = service.create(inventaire, productFournisseur, 10, 8, new BigDecimal("10.00"));

        assertThat(result.getQuantiteTheorique()).isEqualTo(10);
        assertThat(result.getQuantiteReelle()).isEqualTo(8);
        assertThat(result.getEcart()).isEqualTo(-2);
        verify(ligneInventaireDomainService).create(inventaire, productFournisseur, 10, 8, new BigDecimal("10.00"));
    }

    @Test
    void findLigne_should_delegate_to_domain_service() {
        LigneInventaire ligne = buildLigne(10, 10);
        when(ligneInventaireDomainService.findLigne(ligneId)).thenReturn(ligne);

        assertThat(service.findLigne(ligneId)).isSameAs(ligne);
        verify(ligneInventaireDomainService).findLigne(ligneId);
    }

    @Test
    void findByInventaireIdAndProductFournisseurId_should_return_existing_ligne() {
        LigneInventaire ligne = buildLigne(5, 5);
        when(ligneInventaireDomainService.findByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId))
                .thenReturn(Optional.of(ligne));

        Optional<LigneInventaire> result = service.findByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId);

        assertThat(result).isPresent();
        verify(ligneInventaireDomainService).findByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId);
    }

    @Test
    void findByInventaireIdAndProductFournisseurId_should_return_empty_when_not_found() {
        when(ligneInventaireDomainService.findByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId))
                .thenReturn(Optional.empty());

        assertThat(service.findByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId)).isEmpty();
    }

    @Test
    void findAllByInventaireId_should_return_all_lignes() {
        List<LigneInventaire> lignes = List.of(buildLigne(10, 8), buildLigne(5, 5));
        when(ligneInventaireDomainService.findAllByInventaireId(inventaireId)).thenReturn(lignes);

        assertThat(service.findAllByInventaireId(inventaireId)).hasSize(2);
        verify(ligneInventaireDomainService).findAllByInventaireId(inventaireId);
    }

    @Test
    void findResponsesByInventaireId_should_delegate_with_pageable() {
        var pageable = PageRequest.of(0, 10);
        Page<LigneInventaireResponse> page = new PageImpl<>(List.of(), pageable, 0);
        when(ligneInventaireDomainService.findResponsesByInventaireId(inventaireId, pageable)).thenReturn(page);

        assertThat(service.findResponsesByInventaireId(inventaireId, pageable).getContent()).isEmpty();
        verify(ligneInventaireDomainService).findResponsesByInventaireId(inventaireId, pageable);
    }

    @Test
    void updateQuantiteReelle_should_delegate_and_return_updated_ligne() {
        LigneInventaire ligne = buildLigne(10, 8);
        LigneInventaire updated = buildLigne(10, 6);
        when(ligneInventaireDomainService.updateQuantiteReelle(ligne, 6)).thenReturn(updated);

        LigneInventaire result = service.updateQuantiteReelle(ligne, 6);

        assertThat(result.getQuantiteReelle()).isEqualTo(6);
        assertThat(result.getEcart()).isEqualTo(-4);
        verify(ligneInventaireDomainService).updateQuantiteReelle(ligne, 6);
    }

    @Test
    void delete_should_delegate_to_domain_service() {
        LigneInventaire ligne = buildLigne(10, 8);

        service.delete(ligne);

        verify(ligneInventaireDomainService).delete(ligne);
    }

    @Test
    void existsByInventaireIdAndProductFournisseurId_should_return_true_when_exists() {
        when(ligneInventaireDomainService.existsByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId))
                .thenReturn(true);

        assertThat(service.existsByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId)).isTrue();
        verify(ligneInventaireDomainService).existsByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId);
    }

    @Test
    void existsByInventaireIdAndProductFournisseurId_should_return_false_when_absent() {
        when(ligneInventaireDomainService.existsByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId))
                .thenReturn(false);

        assertThat(service.existsByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId)).isFalse();
    }
}
