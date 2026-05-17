package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.application.service.impl.SubscriptionTypeServiceImpl;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.model.TypeAbonnement;
import org.store.abonnement.domain.service.TypeAbonnementDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.UniqueResourceException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionTypeServiceImplTest {

    @Mock private TypeAbonnementDomainService typeAbonnementDomainService;
    @Mock private org.store.common.service.ValidatorService validatorService;

    @InjectMocks
    private SubscriptionTypeServiceImpl service;

    private UUID typeId;

    @BeforeEach
    void setUp() {
        typeId = UUID.randomUUID();
    }

    private SubscriptionTypeRequest validRequest() {
        return new SubscriptionTypeRequest(
                "Mensuel", 1, "POURCENTAGE", new BigDecimal("0"),
                false, true, 10);
    }

    private TypeAbonnement sampleType() {
        TypeAbonnement type = new TypeAbonnement();
        type.setId(typeId);
        type.setNom("Mensuel");
        type.setDureeMois(1);
        type.setReductionType(ReductionType.POURCENTAGE);
        type.setValeurReduction(new BigDecimal("0"));
        type.setRecommande(false);
        type.setActif(true);
        type.setOrdre(10);
        return type;
    }

    @Test
    void create_should_persist_when_nom_available_and_reduction_valid() {
        SubscriptionTypeRequest request = validRequest();
        when(typeAbonnementDomainService.existsByNom("Mensuel")).thenReturn(false);
        when(typeAbonnementDomainService.create(request)).thenReturn(sampleType());

        SubscriptionTypeResponse response = service.create(request);

        assertThat(response.nom()).isEqualTo("Mensuel");
        assertThat(response.dureeMois()).isEqualTo(1);
    }

    @Test
    void create_should_throw_when_nom_taken() {
        when(typeAbonnementDomainService.existsByNom("Mensuel")).thenReturn(true);

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(UniqueResourceException.class);

        verify(typeAbonnementDomainService, never()).create(any());
    }

    @Test
    void create_should_throw_when_reduction_type_without_value() {
        SubscriptionTypeRequest request = new SubscriptionTypeRequest(
                "Mensuel", 1, "POURCENTAGE", null, false, true, 0);
        when(typeAbonnementDomainService.existsByNom("Mensuel")).thenReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void create_should_throw_when_percentage_above_100() {
        SubscriptionTypeRequest request = new SubscriptionTypeRequest(
                "Annuel", 12, "POURCENTAGE", new BigDecimal("150"), false, true, 0);
        when(typeAbonnementDomainService.existsByNom("Annuel")).thenReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void create_should_throw_when_value_without_reduction_type() {
        SubscriptionTypeRequest request = new SubscriptionTypeRequest(
                "Annuel", 12, null, new BigDecimal("10"), false, true, 0);
        when(typeAbonnementDomainService.existsByNom("Annuel")).thenReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void findResponseById_should_return_response() {
        when(typeAbonnementDomainService.findById(typeId)).thenReturn(sampleType());

        SubscriptionTypeResponse response = service.findResponseById(typeId);

        assertThat(response.id()).isEqualTo(typeId);
    }

    @Test
    void findAll_should_delegate() {
        SubscriptionTypeFilter filter = new SubscriptionTypeFilter(null, null, null, 0, 10);
        Page<SubscriptionTypeResponse> page = new PageImpl<>(List.of());
        when(typeAbonnementDomainService.findResponses(filter)).thenReturn(page);

        assertThat(service.findAll(filter)).isSameAs(page);
    }

    @Test
    void update_should_apply_and_save() {
        TypeAbonnement type = sampleType();
        SubscriptionTypeRequest request = new SubscriptionTypeRequest(
                "Annuel", 12, "POURCENTAGE", new BigDecimal("15"),
                true, true, 20);

        when(typeAbonnementDomainService.findById(typeId)).thenReturn(type);
        when(typeAbonnementDomainService.existsByNom("Annuel")).thenReturn(false);
        when(typeAbonnementDomainService.applyRequest(type, request)).thenReturn(type);
        when(typeAbonnementDomainService.save(type)).thenReturn(type);

        service.update(typeId, request);

        verify(typeAbonnementDomainService).applyRequest(type, request);
        verify(typeAbonnementDomainService).save(type);
    }

    @Test
    void update_should_skip_unicity_when_nom_unchanged() {
        TypeAbonnement type = sampleType();
        SubscriptionTypeRequest request = validRequest();
        when(typeAbonnementDomainService.findById(typeId)).thenReturn(type);
        when(typeAbonnementDomainService.applyRequest(type, request)).thenReturn(type);
        when(typeAbonnementDomainService.save(type)).thenReturn(type);

        service.update(typeId, request);

        verify(typeAbonnementDomainService, never()).existsByNom(any());
    }

    @Test
    void activate_should_set_actif_true() {
        TypeAbonnement type = sampleType();
        type.setActif(false);
        when(typeAbonnementDomainService.findById(typeId)).thenReturn(type);
        when(typeAbonnementDomainService.setActive(type, true)).thenAnswer(inv -> {
            type.setActif(true);
            return type;
        });

        SubscriptionTypeResponse response = service.activate(typeId);

        assertThat(response.actif()).isTrue();
    }

    @Test
    void deactivate_should_set_actif_false() {
        TypeAbonnement type = sampleType();
        when(typeAbonnementDomainService.findById(typeId)).thenReturn(type);
        when(typeAbonnementDomainService.setActive(type, false)).thenAnswer(inv -> {
            type.setActif(false);
            return type;
        });

        SubscriptionTypeResponse response = service.deactivate(typeId);

        assertThat(response.actif()).isFalse();
    }

    @Test
    void delete_should_remove() {
        TypeAbonnement type = sampleType();
        when(typeAbonnementDomainService.findById(typeId)).thenReturn(type);

        service.delete(typeId);

        verify(typeAbonnementDomainService).delete(type);
    }
}
