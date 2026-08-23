package org.store.plateforme.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.UniqueResourceException;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.application.service.impl.CategoryDepensePlateformeServiceImpl;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.service.CategoryDepensePlateformeDomainService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryDepensePlateformeServiceImplTest {

    @Mock private CategoryDepensePlateformeDomainService domainService;
    @InjectMocks private CategoryDepensePlateformeServiceImpl service;

    @Test
    void create_should_persist_when_nom_available() {
        CategoryDepensePlateformeRequest request = new CategoryDepensePlateformeRequest("Hébergement", "Serveurs cloud", true);
        CategoryDepensePlateforme saved = new CategoryDepensePlateforme();
        saved.setId(UUID.randomUUID());
        saved.setNom("Hébergement");
        saved.setActif(true);

        when(domainService.existsByNom("Hébergement")).thenReturn(false);
        when(domainService.create(request)).thenReturn(saved);

        CategoryDepensePlateformeResponse response = service.create(request);

        assertThat(response.nom()).isEqualTo("Hébergement");
    }

    @Test
    void create_should_throw_when_nom_already_taken() {
        CategoryDepensePlateformeRequest request = new CategoryDepensePlateformeRequest("Hébergement", null, true);
        when(domainService.existsByNom("Hébergement")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UniqueResourceException.class);
    }
}
