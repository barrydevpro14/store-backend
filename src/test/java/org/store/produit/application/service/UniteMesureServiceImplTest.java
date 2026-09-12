package org.store.produit.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.common.exceptions.UniqueResourceException;
import org.store.produit.application.dto.UniteMesureFilter;
import org.store.produit.application.dto.UniteMesureRequest;
import org.store.produit.application.dto.UniteMesureResponse;
import org.store.produit.application.service.impl.UniteMesureServiceImpl;
import org.store.produit.domain.model.UniteMesure;
import org.store.produit.domain.service.UniteMesureDomainService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniteMesureServiceImplTest {

    @Mock
    private UniteMesureDomainService uniteMesureDomainService;

    @InjectMocks
    private UniteMesureServiceImpl service;

    private UUID uniteId;

    @BeforeEach
    void setUp() {
        uniteId = UUID.randomUUID();
    }

    private UniteMesure sample() {
        UniteMesure unite = new UniteMesure();
        unite.setId(uniteId);
        unite.setCode("KG");
        unite.setLibelle("Kilogramme");
        unite.setSymbole("kg");
        return unite;
    }

    @Test
    void create_should_persist_when_code_available() {
        UniteMesureRequest request = new UniteMesureRequest("kg", "Kilogramme", "kg");
        UniteMesure created = sample();

        when(uniteMesureDomainService.existsByCode("kg")).thenReturn(false);
        when(uniteMesureDomainService.create(request)).thenReturn(created);

        UniteMesureResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(uniteId);
        assertThat(response.code()).isEqualTo("KG");
    }

    @Test
    void create_should_throw_when_code_already_exists() {
        UniteMesureRequest request = new UniteMesureRequest("KG", "Kilogramme", "kg");
        when(uniteMesureDomainService.existsByCode("KG")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UniqueResourceException.class);

        verify(uniteMesureDomainService, never()).create(any());
    }

    @Test
    void findResponseById_should_return_dto() {
        when(uniteMesureDomainService.findById(uniteId)).thenReturn(sample());

        UniteMesureResponse response = service.findResponseById(uniteId);

        assertThat(response.id()).isEqualTo(uniteId);
        assertThat(response.libelle()).isEqualTo("Kilogramme");
    }

    @Test
    void findAll_should_paginate() {
        UniteMesureFilter filter = new UniteMesureFilter(null, null, null, null, 0, 10);
        UniteMesureResponse sampleResponse = new UniteMesureResponse(sample());
        Page<UniteMesureResponse> page = new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 10), 1);

        when(uniteMesureDomainService.findResponsesByFilter(filter)).thenReturn(page);

        Page<UniteMesureResponse> result = service.findAll(filter);

        assertThat(result.getContent()).containsExactly(sampleResponse);
    }

    @Test
    void listAll_should_return_summaries_ordered() {
        when(uniteMesureDomainService.findAllOrdered()).thenReturn(List.of(sample()));

        var result = service.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(uniteId);
    }

    @Test
    void update_should_change_libelle_and_symbole() {
        UniteMesure unite = sample();
        UniteMesureRequest request = new UniteMesureRequest("KG", "Kilo", "Kg");

        when(uniteMesureDomainService.findById(uniteId)).thenReturn(unite);
        when(uniteMesureDomainService.save(any(UniteMesure.class))).thenAnswer(inv -> inv.getArgument(0));

        UniteMesureResponse response = service.update(uniteId, request);

        assertThat(response.libelle()).isEqualTo("Kilo");
        assertThat(response.symbole()).isEqualTo("Kg");
        assertThat(response.code()).isEqualTo("KG");
    }

    @Test
    void delete_should_remove() {
        UniteMesure unite = sample();
        when(uniteMesureDomainService.findById(uniteId)).thenReturn(unite);

        service.delete(uniteId);

        verify(uniteMesureDomainService).delete(unite);
    }

    @Test
    void ensureCodeAvailable_should_throw_when_taken() {
        when(uniteMesureDomainService.existsByCode(eq("KG"))).thenReturn(true);

        assertThatThrownBy(() -> service.ensureCodeAvailable("KG"))
                .isInstanceOf(UniqueResourceException.class);
    }

    @Test
    void resolveIdOrPiece_should_resolve_by_code_or_symbole() {
        when(uniteMesureDomainService.findByCodeOrSymboleOptional("kg")).thenReturn(Optional.of(sample()));

        UUID resolved = service.resolveIdOrPiece("kg", new UUID[1]);

        assertThat(resolved).isEqualTo(uniteId);
    }

    @Test
    void resolveIdOrPiece_should_fallback_to_piece_when_code_blank() {
        UniteMesure piece = sample();
        piece.setId(UUID.randomUUID());
        piece.setCode("PIECE");
        when(uniteMesureDomainService.findByCode("PIECE")).thenReturn(piece);

        UUID resolved = service.resolveIdOrPiece(null, new UUID[1]);

        assertThat(resolved).isEqualTo(piece.getId());
    }
}
