package org.store.plateforme.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.common.exceptions.UniqueResourceException;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.application.service.impl.CategoryDepensePlateformeServiceImpl;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.service.CategoryDepensePlateformeDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryDepensePlateformeServiceImplTest {

    @Mock private CategoryDepensePlateformeDomainService domainService;
    @Mock private IAuditEventPublisher auditEventPublisher;
    @Mock private ICurrentUserService currentUserService;
    @InjectMocks private CategoryDepensePlateformeServiceImpl service;

    private static UserPrincipal callerFixture() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), null, null, "admin", null, null, "ADMIN", List.of());
    }

    @Test
    void create_should_persist_when_nom_available() {
        CategoryDepensePlateformeRequest request = new CategoryDepensePlateformeRequest("Hébergement", "Serveurs cloud", true);
        CategoryDepensePlateforme saved = new CategoryDepensePlateforme();
        saved.setId(UUID.randomUUID());
        saved.setNom("Hébergement");
        saved.setActif(true);

        when(domainService.existsByNom("Hébergement")).thenReturn(false);
        when(domainService.create(request)).thenReturn(saved);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        CategoryDepensePlateformeResponse response = service.create(request);

        assertThat(response.nom()).isEqualTo("Hébergement");
    }

    @Test
    void create_should_throw_when_nom_already_taken() {
        CategoryDepensePlateformeRequest request = new CategoryDepensePlateformeRequest("Hébergement", null, true);
        when(domainService.existsByNom("Hébergement")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UniqueResourceException.class);

        verify(auditEventPublisher, never()).publish(any());
    }

    @Test
    void create_should_publish_audit_event_on_success() {
        CategoryDepensePlateformeRequest request = new CategoryDepensePlateformeRequest("Hébergement", "Serveurs cloud", true);
        CategoryDepensePlateforme saved = new CategoryDepensePlateforme();
        saved.setId(UUID.randomUUID());
        saved.setNom("Hébergement");
        saved.setActif(true);

        when(domainService.existsByNom("Hébergement")).thenReturn(false);
        when(domainService.create(request)).thenReturn(saved);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        service.create(request);

        verify(auditEventPublisher, times(1)).publish(argThat(event ->
                event.action() == AuditAction.CATEGORY_DEPENSE_PLATEFORME_CREATED
                        && event.entityType() == AuditEntityType.CATEGORY_DEPENSE_PLATEFORME));
    }

    @Test
    void update_should_publish_audit_event_on_success() {
        UUID id = UUID.randomUUID();
        CategoryDepensePlateforme existing = new CategoryDepensePlateforme();
        existing.setId(id);
        existing.setNom("Hébergement");
        existing.setActif(true);
        CategoryDepensePlateformeRequest request = new CategoryDepensePlateformeRequest("Hébergement", "Nouvelle description", true);

        when(domainService.findById(id)).thenReturn(existing);
        when(domainService.save(existing)).thenReturn(existing);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        service.update(id, request);

        verify(auditEventPublisher, times(1)).publish(argThat(event ->
                event.action() == AuditAction.CATEGORY_DEPENSE_PLATEFORME_UPDATED
                        && event.entityType() == AuditEntityType.CATEGORY_DEPENSE_PLATEFORME));
    }

    @Test
    void delete_should_deactivate_instead_of_hard_delete() {
        UUID id = UUID.randomUUID();
        CategoryDepensePlateforme existing = new CategoryDepensePlateforme();
        existing.setId(id);
        existing.setNom("Hébergement");
        existing.setActif(true);

        when(domainService.findById(id)).thenReturn(existing);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        service.delete(id);

        verify(domainService).save(argThat(c -> !c.isActif()));
        verify(domainService, never()).delete(any());
    }

    @Test
    void delete_should_publish_audit_event() {
        UUID id = UUID.randomUUID();
        CategoryDepensePlateforme existing = new CategoryDepensePlateforme();
        existing.setId(id);
        existing.setNom("Hébergement");
        existing.setActif(true);

        when(domainService.findById(id)).thenReturn(existing);
        when(currentUserService.getCurrent()).thenReturn(callerFixture());

        service.delete(id);

        verify(auditEventPublisher, times(1)).publish(argThat(event ->
                event.action() == AuditAction.CATEGORY_DEPENSE_PLATEFORME_DELETED
                        && event.entityType() == AuditEntityType.CATEGORY_DEPENSE_PLATEFORME));
    }
}
