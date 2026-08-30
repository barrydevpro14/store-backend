package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.store.abonnement.application.dto.PreuvePaiementRequest;
import org.store.abonnement.application.dto.PreuvePaiementResponse;
import org.store.abonnement.application.dto.RejectPaiementRequest;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.application.service.IPreuvePaiementService;
import org.store.abonnement.domain.enums.StatutPreuvePaiement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.PreuvePaiement;
import org.store.abonnement.domain.service.PaiementAbonnementDomainService;
import org.store.abonnement.domain.service.PreuvePaiementDomainService;
import org.store.common.dto.ImageDownloadResponse;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.model.PieceJointe;
import org.store.common.service.IUploadFileService;
import org.store.notification.application.event.PaiementAbonnementRejectedEvent;
import org.store.notification.application.event.PaiementAbonnementSubmittedEvent;
import org.store.notification.application.service.INotificationEventPublisher;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.paiement.application.service.IFacturationService;
import org.store.paiement.domain.model.Facturation;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Manages PreuvePaiement: submission by the OWNER, validation/rejection by the ADMIN. Rejecting a
 * preuve never mutates the parent facture — only IPaiementAbonnementService.confirmPaiement
 * (called on validate) does that.
 */
@Service
@Transactional(readOnly = true)
public class PreuvePaiementServiceImpl implements IPreuvePaiementService {

    private final PreuvePaiementDomainService preuvePaiementDomainService;
    private final PaiementAbonnementDomainService paiementAbonnementDomainService;
    private final IPaiementAbonnementService paiementAbonnementService;
    private final IUploadFileService uploadFileService;
    private final IFacturationService facturationService;
    private final ICurrentUserService currentUserService;
    private final INotificationEventPublisher notificationEventPublisher;
    private final IAuditEventPublisher auditEventPublisher;

    public PreuvePaiementServiceImpl(PreuvePaiementDomainService preuvePaiementDomainService,
                                     PaiementAbonnementDomainService paiementAbonnementDomainService,
                                     IPaiementAbonnementService paiementAbonnementService,
                                     IUploadFileService uploadFileService,
                                     IFacturationService facturationService,
                                     ICurrentUserService currentUserService,
                                     INotificationEventPublisher notificationEventPublisher,
                                     IAuditEventPublisher auditEventPublisher) {
        this.preuvePaiementDomainService = preuvePaiementDomainService;
        this.paiementAbonnementDomainService = paiementAbonnementDomainService;
        this.paiementAbonnementService = paiementAbonnementService;
        this.uploadFileService = uploadFileService;
        this.facturationService = facturationService;
        this.currentUserService = currentUserService;
        this.notificationEventPublisher = notificationEventPublisher;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Override
    @Transactional
    public PreuvePaiementResponse create(UUID paiementAbonnementId, PreuvePaiementRequest request, MultipartFile file) {
        PaiementAbonnement facture = paiementAbonnementDomainService.findById(paiementAbonnementId);
        paiementAbonnementService.ensurePaiementAccessibleByCaller(facture);
        paiementAbonnementService.ensurePaiementIsFactureGeneree(facture);
        ensureNoPendingPreuve(paiementAbonnementId);

        PreuvePaiement preuve = new PreuvePaiement();
        preuve.setPaiementAbonnement(facture);
        preuve.setDate(LocalDate.now());
        Facturation facturation = facturationService.findByIdAvailableForCurrentCountry(request.facturationId());
        preuve.setMoyen(facturation.getMoyenPaiement());
        preuve.setReferenceTransaction(request.referenceTransaction());
        if (file != null && !file.isEmpty()) {
            PieceJointe image = uploadFileService.buildImage(file);
            preuve.setPreuve(image);
        }
        preuve.setStatut(StatutPreuvePaiement.EN_ATTENTE_VALIDATION);

        PreuvePaiement saved = preuvePaiementDomainService.save(preuve);

        String sigle = facture.getAbonnement().getEntreprise() != null
                ? facture.getAbonnement().getEntreprise().getSigle() : null;
        notificationEventPublisher.publishPaiementSubmitted(
                new PaiementAbonnementSubmittedEvent(facture.getId(), sigle, facture.getMontantFinal()));

        return new PreuvePaiementResponse(saved);
    }

    @Override
    @Transactional
    public PreuvePaiementResponse validate(UUID preuveId) {
        PreuvePaiement preuve = preuvePaiementDomainService.findById(preuveId);
        ensurePreuveIsPendingValidation(preuve);

        PreuvePaiement validated = preuvePaiementDomainService.markAsValidee(preuve);
        paiementAbonnementService.confirmPaiement(validated.getPaiementAbonnement().getId(), validated.getDate());

        return new PreuvePaiementResponse(validated);
    }

    @Override
    @Transactional
    public PreuvePaiementResponse reject(UUID preuveId, RejectPaiementRequest request) {
        PreuvePaiement preuve = preuvePaiementDomainService.findById(preuveId);
        ensurePreuveIsPendingValidation(preuve);

        PreuvePaiement rejected = preuvePaiementDomainService.markAsRejetee(preuve, request.motifRejet());

        PaiementAbonnement facture = rejected.getPaiementAbonnement();
        UUID entrepriseId = facture.getAbonnement().getEntreprise().getId();
        String entrepriseSigle = facture.getAbonnement().getEntreprise().getSigle();

        notificationEventPublisher.publishPaiementRejected(
                new PaiementAbonnementRejectedEvent(facture.getId(), entrepriseId, request.motifRejet()));

        UserPrincipal caller = currentUserService.getCurrent();
        auditEventPublisher.publish(new AuditEvent(
                AuditAction.PAIEMENT_ABONNEMENT_REJECTED, AuditEntityType.PAIEMENT_ABONNEMENT,
                rejected.getId(), entrepriseSigle,
                caller.accountId().toString(), caller.username(), caller.entrepriseId(), null, null));

        return new PreuvePaiementResponse(rejected);
    }

    @Override
    public ImageDownloadResponse getImage(UUID preuveId) {
        PreuvePaiement preuve = preuvePaiementDomainService.findById(preuveId);
        paiementAbonnementService.ensurePaiementAccessibleByCaller(preuve.getPaiementAbonnement());

        PieceJointe image = preuve.getPreuve();
        if (image == null) {
            throw new EntityException("paiementAbonnement.preuve.notFound");
        }
        return new ImageDownloadResponse(image.getDocument(), image.getContentType());
    }

    private void ensureNoPendingPreuve(UUID paiementAbonnementId) {
        if (preuvePaiementDomainService.existsPendingForFacture(paiementAbonnementId)) {
            throw new BadArgumentException("paiementAbonnement.alreadyPending");
        }
    }

    private void ensurePreuveIsPendingValidation(PreuvePaiement preuve) {
        if (preuve.getStatut() != StatutPreuvePaiement.EN_ATTENTE_VALIDATION) {
            throw new BadArgumentException("paiementAbonnement.notPendingValidation");
        }
    }
}
