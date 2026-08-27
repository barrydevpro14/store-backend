package org.store.abonnement.application.service;

import org.springframework.web.multipart.MultipartFile;
import org.store.abonnement.application.dto.PreuvePaiementRequest;
import org.store.abonnement.application.dto.PreuvePaiementResponse;
import org.store.abonnement.application.dto.RejectPaiementRequest;
import org.store.common.dto.ImageDownloadResponse;

import java.util.UUID;

public interface IPreuvePaiementService {

    /**
     * OWNER submits proof against an existing FACTURE_GENEREE/EN_RETARD facture. Creates a
     * PreuvePaiement (EN_ATTENTE_VALIDATION) — the facture itself is not modified. Throws
     * BadArgumentException when a PreuvePaiement is already EN_ATTENTE_VALIDATION for this facture.
     */
    PreuvePaiementResponse create(UUID paiementAbonnementId, PreuvePaiementRequest request, MultipartFile file);

    /**
     * ADMIN validates: preuve -> VALIDEE, then confirms the parent facture (VALIDE, abonnement
     * activated/extended, revenue recorded) via IPaiementAbonnementService.confirmPaiement.
     */
    PreuvePaiementResponse validate(UUID preuveId);

    /**
     * ADMIN rejects: preuve -> REJETEE with mandatory reason. The facture is untouched — the owner
     * may resubmit a new preuve immediately, and the reserved coupon stays applied.
     */
    PreuvePaiementResponse reject(UUID preuveId, RejectPaiementRequest request);

    /** Downloads the proof image attached to a preuve. */
    ImageDownloadResponse getImage(UUID preuveId);
}
