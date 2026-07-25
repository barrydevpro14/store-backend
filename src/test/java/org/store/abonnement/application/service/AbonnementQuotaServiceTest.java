package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.magasin.domain.service.MagasinDomainService;
import org.store.users.domain.service.EmployeDomainService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbonnementQuotaServiceTest {

    @Mock private AbonnementDomainService abonnementDomainService;
    @Mock private MagasinDomainService magasinDomainService;
    @Mock private EmployeDomainService employeDomainService;

    @InjectMocks
    private AbonnementQuotaService service;

    private UUID entrepriseId;
    private UUID magasinId;
    private Abonnement abonnement;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();

        PlanAbonnement plan = new PlanAbonnement();
        plan.setNombreMagasinsMax(3);
        plan.setNombreEmployesMax(5);

        abonnement = new Abonnement();
        abonnement.setPlanAbonnement(plan);
    }

    @Test
    void ensureMagasinQuota_should_skip_when_no_active_abonnement() {
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> service.ensureMagasinQuota(entrepriseId));
    }

    @Test
    void ensureMagasinQuota_should_skip_when_quota_unlimited() {
        abonnement.getPlanAbonnement().setNombreMagasinsMax(0);
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.of(abonnement));

        assertThatNoException().isThrownBy(() -> service.ensureMagasinQuota(entrepriseId));
    }

    @Test
    void ensureMagasinQuota_should_pass_when_below_limit() {
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.of(abonnement));
        when(magasinDomainService.countByEntrepriseId(entrepriseId)).thenReturn(2L);

        assertThatNoException().isThrownBy(() -> service.ensureMagasinQuota(entrepriseId));
        verify(magasinDomainService).countByEntrepriseId(entrepriseId);
    }

    @Test
    void ensureMagasinQuota_should_throw_when_limit_reached() {
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.of(abonnement));
        when(magasinDomainService.countByEntrepriseId(entrepriseId)).thenReturn(3L);

        assertThatThrownBy(() -> service.ensureMagasinQuota(entrepriseId))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void ensureEmployeQuota_should_skip_when_no_active_abonnement() {
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> service.ensureEmployeQuota(entrepriseId, magasinId));
    }

    @Test
    void ensureEmployeQuota_should_skip_when_quota_unlimited() {
        abonnement.getPlanAbonnement().setNombreEmployesMax(0);
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.of(abonnement));

        assertThatNoException().isThrownBy(() -> service.ensureEmployeQuota(entrepriseId, magasinId));
    }

    @Test
    void ensureEmployeQuota_should_pass_when_below_limit() {
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.of(abonnement));
        when(employeDomainService.countByMagasinId(magasinId)).thenReturn(4L);

        assertThatNoException().isThrownBy(() -> service.ensureEmployeQuota(entrepriseId, magasinId));
        verify(employeDomainService).countByMagasinId(magasinId);
    }

    @Test
    void ensureEmployeQuota_should_throw_when_limit_reached() {
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.of(abonnement));
        when(employeDomainService.countByMagasinId(magasinId)).thenReturn(5L);

        assertThatThrownBy(() -> service.ensureEmployeQuota(entrepriseId, magasinId))
                .isInstanceOf(BadArgumentException.class);
    }
}
