package org.store.abonnement.application.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import org.store.abonnement.application.dto.PaiementAbonnementFilter;
import org.store.abonnement.application.dto.PaiementAbonnementRequest;
import org.store.abonnement.application.dto.PaiementAbonnementResponse;
import org.store.abonnement.application.dto.RejectPaiementRequest;
import org.store.common.dto.ImageDownloadResponse;

import java.util.UUID;

public interface IPaiementAbonnementService {

    /**
     * PROPRIETAIRE enregistre son paiement (paiement manuel, hors-app) avec preuve image obligatoire.
     * Crée un PaiementAbonnement en EN_ATTENTE_VALIDATION. L'abonnement reste EN_ATTENTE jusqu'à la validation admin.
     */
    PaiementAbonnementResponse create(UUID abonnementId, PaiementAbonnementRequest paiementAbonnementRequest, MultipartFile preuve);

    /**
     * ADMIN valide le paiement : statut → VALIDE, et l'abonnement passe en ACTIF avec `dateDebut`/`dateFin` calculés
     * (today si pas d'abonnement actif courant, sinon `currentActif.dateFin + 1`). dateFin = dateDebut + typeAbonnement.dureeMois.
     */
    PaiementAbonnementResponse validate(UUID paiementId);

    /**
     * ADMIN rejette le paiement : statut → REJETE avec motif obligatoire. L'abonnement reste EN_ATTENTE.
     * Libère le coupon réservé à la souscription (rollback : supprime `UtilisationCoupon` + décrémente `nombreUtilisations`).
     */
    PaiementAbonnementResponse reject(UUID paiementId, RejectPaiementRequest rejectPaiementRequest);

    /**
     * Listing paginé filtré (ADMIN voit tout, PROPRIETAIRE auto-scopé sur son entreprise).
     */
    Page<PaiementAbonnementResponse> findAll(PaiementAbonnementFilter filter);

    /**
     * Lecture par id en `Response`. Scoping sur l'entreprise du caller (sauf ADMIN).
     */
    PaiementAbonnementResponse findResponseById(UUID paiementId);

    /**
     * Téléchargement de la preuve d'image. Scoping sur l'entreprise du caller (sauf ADMIN).
     */
    ImageDownloadResponse getPreuve(UUID paiementId);
}
