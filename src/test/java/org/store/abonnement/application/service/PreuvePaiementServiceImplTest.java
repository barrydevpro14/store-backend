package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.store.abonnement.application.dto.PreuvePaiementRequest;
import org.store.abonnement.application.dto.PreuvePaiementResponse;
import org.store.abonnement.application.dto.RejectPaiementRequest;
import org.store.abonnement.application.service.impl.PreuvePaiementServiceImpl;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.enums.StatutPreuvePaiement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.PreuvePaiement;
import org.store.abonnement.domain.service.PaiementAbonnementDomainService;
import org.store.abonnement.domain.service.PreuvePaiementDomainService;
import org.store.common.dto.ImageDownloadResponse;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.model.PieceJointe;
import org.store.common.service.IUploadFileService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreuvePaiementServiceImplTest {

    @Mock private PreuvePaiementDomainService preuvePaiementDomainService;
    @Mock private PaiementAbonnementDomainService paiementAbonnementDomainService;
    @Mock private IPaiementAbonnementService paiementAbonnementService;
    @Mock private IUploadFileService uploadFileService;
    @Mock private IMoyenPaiementService moyenPaiementService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private org.store.notification.application.service.INotificationEventPublisher notificationEventPublisher;
    @Mock private org.store.audit.application.service.IAuditEventPublisher auditEventPublisher;

    @InjectMocks
    private PreuvePaiementServiceImpl service;

    private UUID factureId;
    private UUID preuveId;
    private UUID entrepriseId;
    private PaiementAbonnement facture;

    @BeforeEach
    void setUp() {
        factureId = UUID.randomUUID();
        preuveId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        entreprise.setSigle("ACME");

        Abonnement abonnement = new Abonnement();
        abonnement.setId(UUID.randomUUID());
        abonnement.setEntreprise(entreprise);

        facture = new PaiementAbonnement();
        facture.setId(factureId);
        facture.setAbonnement(abonnement);
        facture.setStatut(StatutPaiementAbonnement.FACTURE_GENEREE);
        facture.setMontantFinal(new BigDecimal("19900"));
    }

    private UserPrincipal admin() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "admin", null, null, "ADMIN", List.of("ADMIN_ACCESS", "SUBSCRIPTION_VALIDATE"));
    }

    private MultipartFile validFile() {
        return new MockMultipartFile("file", "preuve.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void create_should_persist_preuve_en_attente_validation() {
        PreuvePaiementRequest request = new PreuvePaiementRequest(UUID.randomUUID(), "TXN-123");
        MoyenPaiement moyen = new MoyenPaiement();
        moyen.setId(request.moyenPaiementId());

        when(paiementAbonnementDomainService.findById(factureId)).thenReturn(facture);
        when(preuvePaiementDomainService.existsPendingForFacture(factureId)).thenReturn(false);
        when(moyenPaiementService.findById(request.moyenPaiementId())).thenReturn(moyen);
        when(uploadFileService.buildImage(any())).thenReturn(new PieceJointe());
        when(preuvePaiementDomainService.save(any(PreuvePaiement.class))).thenAnswer(inv -> inv.getArgument(0));

        PreuvePaiementResponse response = service.create(factureId, request, validFile());

        assertThat(response.statut()).isEqualTo(StatutPreuvePaiement.EN_ATTENTE_VALIDATION);
        assertThat(response.referenceTransaction()).isEqualTo("TXN-123");
        assertThat(response.date()).isEqualTo(LocalDate.now());
        assertThat(facture.getStatut()).isEqualTo(StatutPaiementAbonnement.FACTURE_GENEREE);
        verify(paiementAbonnementService).ensurePaiementAccessibleByCaller(facture);
        verify(paiementAbonnementService).ensurePaiementIsFactureGeneree(facture);
    }

    @Test
    void create_should_throw_when_a_preuve_is_already_pending() {
        PreuvePaiementRequest request = new PreuvePaiementRequest(UUID.randomUUID(), "TXN-123");
        when(paiementAbonnementDomainService.findById(factureId)).thenReturn(facture);
        when(preuvePaiementDomainService.existsPendingForFacture(factureId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(factureId, request, null))
                .isInstanceOf(BadArgumentException.class);

        verify(preuvePaiementDomainService, never()).save(any());
    }

    @Test
    void validate_should_mark_preuve_validee_and_confirm_facture() {
        PreuvePaiement preuve = new PreuvePaiement();
        preuve.setId(preuveId);
        preuve.setPaiementAbonnement(facture);
        preuve.setDate(LocalDate.now());
        preuve.setStatut(StatutPreuvePaiement.EN_ATTENTE_VALIDATION);

        when(preuvePaiementDomainService.findById(preuveId)).thenReturn(preuve);
        when(preuvePaiementDomainService.markAsValidee(preuve)).thenAnswer(inv -> {
            preuve.setStatut(StatutPreuvePaiement.VALIDEE);
            return preuve;
        });

        PreuvePaiementResponse response = service.validate(preuveId);

        assertThat(response.statut()).isEqualTo(StatutPreuvePaiement.VALIDEE);
        verify(paiementAbonnementService).confirmPaiement(factureId, preuve.getDate());
    }

    @Test
    void validate_should_throw_when_preuve_not_pending() {
        PreuvePaiement preuve = new PreuvePaiement();
        preuve.setStatut(StatutPreuvePaiement.VALIDEE);
        when(preuvePaiementDomainService.findById(preuveId)).thenReturn(preuve);

        assertThatThrownBy(() -> service.validate(preuveId))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void reject_should_mark_preuve_rejetee_and_not_touch_facture() {
        PreuvePaiement preuve = new PreuvePaiement();
        preuve.setId(preuveId);
        preuve.setPaiementAbonnement(facture);
        preuve.setStatut(StatutPreuvePaiement.EN_ATTENTE_VALIDATION);

        when(preuvePaiementDomainService.findById(preuveId)).thenReturn(preuve);
        when(preuvePaiementDomainService.markAsRejetee(preuve, "Preuve illisible")).thenAnswer(inv -> {
            preuve.setStatut(StatutPreuvePaiement.REJETEE);
            preuve.setMotifRejet("Preuve illisible");
            return preuve;
        });
        lenient().when(currentUserService.getCurrent()).thenReturn(admin());

        PreuvePaiementResponse response = service.reject(preuveId, new RejectPaiementRequest("Preuve illisible"));

        assertThat(response.statut()).isEqualTo(StatutPreuvePaiement.REJETEE);
        assertThat(response.motifRejet()).isEqualTo("Preuve illisible");
        assertThat(facture.getStatut()).isEqualTo(StatutPaiementAbonnement.FACTURE_GENEREE);
        verify(paiementAbonnementService, never()).confirmPaiement(any(), any());
    }

    @Test
    void getImage_should_return_image_and_check_access_via_parent_facture() {
        PieceJointe image = new PieceJointe();
        image.setDocument(new byte[]{1, 2, 3});
        image.setContentType("image/png");

        PreuvePaiement preuve = new PreuvePaiement();
        preuve.setId(preuveId);
        preuve.setPaiementAbonnement(facture);
        preuve.setPreuve(image);

        when(preuvePaiementDomainService.findById(preuveId)).thenReturn(preuve);

        ImageDownloadResponse response = service.getImage(preuveId);

        assertThat(response).isEqualTo(new ImageDownloadResponse(image.getDocument(), image.getContentType()));
        verify(paiementAbonnementService).ensurePaiementAccessibleByCaller(facture);
    }

    @Test
    void getImage_should_throw_when_preuve_has_no_image() {
        PreuvePaiement preuve = new PreuvePaiement();
        preuve.setId(preuveId);
        preuve.setPaiementAbonnement(facture);
        preuve.setPreuve(null);

        when(preuvePaiementDomainService.findById(preuveId)).thenReturn(preuve);

        assertThatThrownBy(() -> service.getImage(preuveId))
                .isInstanceOf(EntityException.class);

        verify(paiementAbonnementService).ensurePaiementAccessibleByCaller(facture);
    }
}
