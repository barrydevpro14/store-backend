package org.store.inventaire.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.tools.OwnershipHelper;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.ValidatorService;
import org.store.common.tools.DateHelper;
import org.store.depense.application.dto.DepenseFilter;
import org.store.depense.application.dto.DepenseTotalResponse;
import org.store.depense.application.service.IDepenseService;
import org.store.inventaire.application.dto.BilanInventaireRequest;
import org.store.inventaire.application.dto.CloturerRequest;
import org.store.inventaire.application.dto.InventaireFilter;
import org.store.inventaire.application.dto.InventaireResponse;
import org.store.inventaire.application.dto.LigneInventaireRequest;
import org.store.inventaire.application.dto.LigneInventaireResponse;
import org.store.inventaire.application.dto.LigneInventaireUpdateRequest;
import org.store.inventaire.application.dto.RapportInventaireCommand;
import org.store.inventaire.application.dto.RapportInventaireResponse;
import org.store.inventaire.application.service.IInventaireService;
import org.store.inventaire.application.service.ILigneInventaireService;
import org.store.inventaire.application.service.IRapportInventaireService;
import org.store.inventaire.domain.enums.InventaireStatut;
import org.store.inventaire.domain.enums.TypeInventaire;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.model.LigneInventaire;
import org.store.inventaire.domain.service.InventaireDomainService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.AjustementStockRequest;
import org.store.stock.application.service.IAjustementStockService;
import org.store.stock.application.service.IStockService;
import org.store.stock.domain.enums.MotifAjustement;
import org.store.stock.domain.enums.TypeAjustement;
import org.store.stock.domain.model.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestre l'inventaire (PHYSIQUE ou AUTOMATIQUE) : creation, transitions de statut.
 * PHYSIQUE : saisie ligne par PF, BILAN valorise le stock physique compte, CLOTURE applique
 * les ajustements pour chaque ecart != 0 (motif INVENTAIRE_PHYSIQUE).
 * AUTOMATIQUE : pas de lignes, BILAN valorise le stock via computeValuation (ecart = 0),
 * CLOTURE est une transition directe sans ajustement stock.
 * EN_COURS|BILAN -> ANNULE : abandon sans effet stock ni rapport.
 */
@Service
@Transactional(readOnly = true)
public class InventaireServiceImpl implements IInventaireService {

    private final InventaireDomainService inventaireDomainService;
    private final ILigneInventaireService ligneInventaireService;
    private final IRapportInventaireService rapportInventaireService;
    private final IStockService stockService;
    private final IDepenseService depenseService;
    private final IMagasinService magasinService;
    private final IProductFournisseurService productFournisseurService;
    private final IAjustementStockService ajustementStockService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;
    private final IMessageSourceService messageSourceService;

    public InventaireServiceImpl(InventaireDomainService inventaireDomainService,
                                 ILigneInventaireService ligneInventaireService,
                                 IRapportInventaireService rapportInventaireService,
                                 IStockService stockService,
                                 IDepenseService depenseService,
                                 IMagasinService magasinService,
                                 IProductFournisseurService productFournisseurService,
                                 IAjustementStockService ajustementStockService,
                                 ICurrentUserService currentUserService,
                                 ValidatorService validatorService,
                                 IMessageSourceService messageSourceService) {
        this.inventaireDomainService = inventaireDomainService;
        this.ligneInventaireService = ligneInventaireService;
        this.rapportInventaireService = rapportInventaireService;
        this.stockService = stockService;
        this.depenseService = depenseService;
        this.magasinService = magasinService;
        this.productFournisseurService = productFournisseurService;
        this.ajustementStockService = ajustementStockService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
        this.messageSourceService = messageSourceService;
    }

    /** Cree un inventaire (PHYSIQUE ou AUTOMATIQUE) au statut EN_COURS. Rejette si un inventaire actif existe deja. */
    @Override
    @Transactional
    public InventaireResponse create(UUID magasinId, TypeInventaire type) {
        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(magasinId));
        if (inventaireDomainService.hasActiveInventaire(magasin.getId())) {
            throw new BadArgumentException("inventaire.already.open");
        }
        Inventaire inventaire = inventaireDomainService.create(magasin, LocalDate.now(), type);
        return new InventaireResponse(inventaire);
    }

    /** Saisit une ligne d'inventaire PHYSIQUE : snapshot quantite theorique (SUM lots actifs du PF) + calcul ecart. */
    @Override
    @Transactional
    public LigneInventaireResponse addLigne(UUID inventaireId, LigneInventaireRequest request) {
        validatorService.validate(request);
        UserPrincipal currentUser = currentUserService.getCurrent();
        Inventaire inventaire = inventaireDomainService.findById(inventaireId);
        ensureBelongsToCurrentEntreprise(inventaire, currentUser.entrepriseId());
        ensureStatutEnCours(inventaire);
        ensureTypePhysique(inventaire);

        ProductFournisseur productFournisseur = productFournisseurService.ensureBelongsToCurrentEntreprise(
                productFournisseurService.findById(request.productFournisseurId()));

        Optional<LigneInventaire> existing = ligneInventaireService
                .findByInventaireIdAndProductFournisseurId(inventaireId, productFournisseur.getId());

        if (existing.isPresent()) {
            LigneInventaire ligne = existing.get();
            int nouvQte = ligne.getQuantiteReelle() + request.quantiteReelle();
            ligne.setPrixUnitaire(request.prixUnitaire());
            return new LigneInventaireResponse(ligneInventaireService.updateQuantiteReelle(ligne, nouvQte));
        }

        int quantiteTheorique = computeQuantiteTheorique(inventaire.getMagasin().getId(), productFournisseur.getId());
        LigneInventaire ligne = ligneInventaireService.create(
                inventaire, productFournisseur, quantiteTheorique, request.quantiteReelle(), request.prixUnitaire()
        );
        return new LigneInventaireResponse(ligne);
    }

    /** Modifie la quantite reelle d'une ligne existante (correction de saisie). Statut EN_COURS et type PHYSIQUE uniquement. */
    @Override
    @Transactional
    public LigneInventaireResponse updateLigne(UUID inventaireId, UUID ligneId, LigneInventaireUpdateRequest request) {
        validatorService.validate(request);
        UserPrincipal currentUser = currentUserService.getCurrent();
        Inventaire inventaire = inventaireDomainService.findById(inventaireId);
        ensureBelongsToCurrentEntreprise(inventaire, currentUser.entrepriseId());
        ensureStatutEnCours(inventaire);
        ensureTypePhysique(inventaire);

        LigneInventaire ligne = ligneInventaireService.findLigne(ligneId);
        ensureLigneBelongsToInventaire(ligne, inventaireId);

        LigneInventaire updated = ligneInventaireService.updateQuantiteReelle(ligne, request.quantiteReelle());
        return new LigneInventaireResponse(updated);
    }

    /** Supprime une ligne (correction de saisie). Statut EN_COURS et type PHYSIQUE uniquement. */
    @Override
    @Transactional
    public void deleteLigne(UUID inventaireId, UUID ligneId) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        Inventaire inventaire = inventaireDomainService.findById(inventaireId);
        ensureBelongsToCurrentEntreprise(inventaire, currentUser.entrepriseId());
        ensureStatutEnCours(inventaire);
        ensureTypePhysique(inventaire);

        LigneInventaire ligne = ligneInventaireService.findLigne(ligneId);
        ensureLigneBelongsToInventaire(ligne, inventaireId);

        ligneInventaireService.delete(ligne);
    }

    /** Liste paginee des lignes d'un inventaire, scopee entreprise. */
    @Override
    public Page<LigneInventaireResponse> findLignes(UUID inventaireId, Pageable pageable) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        Inventaire inventaire = inventaireDomainService.findById(inventaireId);
        ensureBelongsToCurrentEntreprise(inventaire, currentUser.entrepriseId());
        return ligneInventaireService.findResponsesByInventaireId(inventaireId, pageable);
    }

    /** Transition EN_COURS -> BILAN. PHYSIQUE : valorise lignes physiques/theoriques. AUTOMATIQUE : valorise via computeValuation (montantPhysique = montantAutomatique, ecart = 0). */
    @Override
    @Transactional
    public InventaireResponse passerEnBilan(UUID inventaireId, BilanInventaireRequest request) {
        validatorService.validate(request);
        UserPrincipal currentUser = currentUserService.getCurrent();
        Inventaire inventaire = inventaireDomainService.findById(inventaireId);
        ensureBelongsToCurrentEntreprise(inventaire, currentUser.entrepriseId());
        ensureStatutEnCours(inventaire);

        Inventaire updated = inventaireDomainService.transitionStatut(inventaire, InventaireStatut.BILAN);

        if (inventaire.getType() == TypeInventaire.AUTOMATIQUE) {
            produireRapportAutomatique(updated, request);
        } else {
            List<LigneInventaire> lignes = ligneInventaireService.findAllByInventaireId(inventaireId);
            produireRapport(updated, lignes, request);
        }

        return new InventaireResponse(updated);
    }

    /** Transition BILAN -> CLOTURE. PHYSIQUE : ajustements stock par ligne ecart != 0. AUTOMATIQUE : transition directe sans ajustement. */
    @Override
    @Transactional
    public InventaireResponse cloturer(UUID inventaireId, CloturerRequest request) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        Inventaire inventaire = inventaireDomainService.findById(inventaireId);
        ensureBelongsToCurrentEntreprise(inventaire, currentUser.entrepriseId());
        ensureStatutBilan(inventaire);

        if (request != null && request.commentaire() != null && !request.commentaire().isBlank()) {
            inventaire.setCommentaire(request.commentaire().trim());
        }

        if (inventaire.getType() == TypeInventaire.PHYSIQUE) {
            List<LigneInventaire> lignes = ligneInventaireService.findAllByInventaireId(inventaireId);
            reconcilierQuantitesTheoriques(inventaire, lignes);
            lignes.stream()
                    .filter(ligne -> ligne.getEcart() != 0)
                    .forEach(ligne -> appliquerAjustement(inventaire, ligne));
        }

        Inventaire updated = inventaireDomainService.transitionStatut(inventaire, InventaireStatut.CLOTURE);
        return new InventaireResponse(updated);
    }

    /** Transition EN_COURS|BILAN -> ANNULE : abandon sans application des ecarts. */
    @Override
    @Transactional
    public InventaireResponse annuler(UUID inventaireId) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        Inventaire inventaire = inventaireDomainService.findById(inventaireId);
        ensureBelongsToCurrentEntreprise(inventaire, currentUser.entrepriseId());
        ensureStatutEnCoursOuBilan(inventaire);
        Inventaire updated = inventaireDomainService.transitionStatut(inventaire, InventaireStatut.ANNULE);
        return new InventaireResponse(updated);
    }

    /** Listing pagine filtre scope entreprise (statut, plage de dates, magasin). */
    @Override
    public Page<InventaireResponse> findAllByCurrentEntreprise(InventaireFilter filter) {
        validatorService.validate(filter);
        UserPrincipal currentUser = currentUserService.getCurrent();
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(filter.magasinId()));
        return inventaireDomainService.findResponsesByFilter(filter, currentUser.entrepriseId());
    }

    /** GET by id avec scoping entreprise (throw notFound si absent ou autre entreprise). */
    @Override
    public InventaireResponse findResponseById(UUID id) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        return inventaireDomainService.findResponseById(id, currentUser.entrepriseId())
                .orElseThrow(() -> new EntityException("inventaire.notFound", id));
    }

    /** Retourne l'inventaire actif (EN_COURS ou BILAN) du magasin demande, ou empty si aucun. */
    @Override
    public java.util.Optional<InventaireResponse> findActiveByMagasinId(UUID magasinId) {
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(magasinId));
        return inventaireDomainService.findActive(magasinId);
    }

    /** Rapport projete de l'inventaire (404 si pas encore cloture ou inventaire absent / autre entreprise). */
    @Override
    public RapportInventaireResponse findRapportByInventaireId(UUID inventaireId) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        return rapportInventaireService.findResponseByInventaireId(inventaireId, currentUser.entrepriseId())
                .orElseThrow(() -> new EntityException("rapport.notFound", inventaireId));
    }

    /** Verifie que l'inventaire appartient a l'entreprise du caller (via magasin.entreprise). */
    public void ensureBelongsToCurrentEntreprise(Inventaire inventaire, UUID entrepriseId) {
        OwnershipHelper.ensureOwnership(
                inventaire,
                inventaire.getMagasin().getEntreprise().getId(),
                entrepriseId,
                "inventaire.notOwned"
        );
    }

    /** Verifie que l'inventaire est au statut EN_COURS (sinon plus modifiable). */
    public void ensureStatutEnCours(Inventaire inventaire) {
        if (inventaire.getStatut() != InventaireStatut.EN_COURS) {
            throw new BadArgumentException("inventaire.statut.notEnCours", inventaire.getStatut());
        }
    }

    /** Verifie que l'inventaire est au statut BILAN (prerequis pour la cloture). */
    public void ensureStatutBilan(Inventaire inventaire) {
        if (inventaire.getStatut() != InventaireStatut.BILAN) {
            throw new BadArgumentException("inventaire.statut.notBilan", inventaire.getStatut());
        }
    }

    /** Rejette toute mutation de lignes sur un inventaire AUTOMATIQUE (pas de comptage physique). */
    public void ensureTypePhysique(Inventaire inventaire) {
        if (inventaire.getType() == TypeInventaire.AUTOMATIQUE) {
            throw new BadArgumentException("inventaire.type.notPhysique");
        }
    }

    /** Verifie que l'inventaire est annulable (EN_COURS ou BILAN, pas CLOTURE/ANNULE). */
    public void ensureStatutEnCoursOuBilan(Inventaire inventaire) {
        InventaireStatut statut = inventaire.getStatut();
        if (statut != InventaireStatut.EN_COURS && statut != InventaireStatut.BILAN) {
            throw new BadArgumentException("inventaire.statut.notAnnulable", statut);
        }
    }

    /** Verifie que la ligne appartient bien a l'inventaire passe en URL (protege contre URL forgee). */
    public void ensureLigneBelongsToInventaire(LigneInventaire ligne, UUID inventaireId) {
        if (!ligne.getInventaire().getId().equals(inventaireId)) {
            throw new BadArgumentException("inventaire.ligne.notMatchingInventaire");
        }
    }

    /** Empeche la saisie d'un meme PF deux fois dans le meme inventaire. */
    public void ensureNotDuplicate(UUID inventaireId, UUID productFournisseurId) {
        if (ligneInventaireService.existsByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId)) {
            throw new BadArgumentException("inventaire.ligne.duplicate");
        }
    }

    /**
     * Met a jour la quantiteTheorique de chaque ligne a partir du stock courant du magasin,
     * puis recalcule l'ecart. Appele au debut de la cloture pour corriger les derives dues
     * aux ventes survenues entre la creation de l'inventaire et sa cloture.
     * Si aucun stock n'existe pour le PF, quantiteTheorique est fixee a 0.
     */
    public void reconcilierQuantitesTheoriques(Inventaire inventaire, List<LigneInventaire> lignes) {
        UUID magasinId = inventaire.getMagasin().getId();

        lignes.forEach(ligne -> {
            int lotsActuels = computeQuantiteTheorique(magasinId, ligne.getProductFournisseur().getId());

            if (lotsActuels != ligne.getQuantiteTheorique()) {
                ligneInventaireService.updateQuantiteTheorique(ligne, lotsActuels);
            }
        });
    }

    /** Stock theorique d'un PF dans un magasin = quantiteDisponible du Stock agrege (toujours synchronise). */
    public int computeQuantiteTheorique(UUID magasinId, UUID productFournisseurId) {
        return stockService.findByMagasinAndProductFournisseur(magasinId, productFournisseurId)
                .map(Stock::getQuantiteDisponible)
                .orElse(0);
    }

    /**
     * Delegue a IAjustementStockService.create pour une ligne d'inventaire avec ecart != 0.
     * POSITIF si surplus (plus compté que théorique), NEGATIF si manque.
     * Pour un NEGATIF, si aucun enregistrement Stock n'existe (incohérence de données),
     * l'ajustement est ignoré — l'écart reste visible dans la ligne d'inventaire.
     */
    public void appliquerAjustement(Inventaire inventaire, LigneInventaire ligne) {
        int ecart = ligne.getEcart();
        ProductFournisseur productFournisseur = ligne.getProductFournisseur();
        TypeAjustement type = ecart > 0 ? TypeAjustement.POSITIF : TypeAjustement.NEGATIF;

        if (type == TypeAjustement.NEGATIF) {
            boolean stockExists = stockService
                    .findByMagasinAndProductFournisseur(inventaire.getMagasin().getId(), productFournisseur.getId())
                    .isPresent();
            if (!stockExists) return;
        }
        AjustementStockRequest request = new AjustementStockRequest(
                inventaire.getMagasin().getId(),
                productFournisseur.getProduct().getId(),
                type,
                Math.abs(ecart),
                productFournisseur.getId(),
                ecart > 0 ? (ligne.getPrixUnitaire() != null ? ligne.getPrixUnitaire() : productFournisseur.getPrixAchat()) : null,
                MotifAjustement.INVENTAIRE_PHYSIQUE,
                messageSourceService.getMessage("inventaire.cloture.commentaire", new Object[]{inventaire.getId()})
        );
        ajustementStockService.create(request);
    }

    /** Produit le rapport d'un inventaire AUTOMATIQUE : montantAutomatique = valeur totale stock via computeValuation, montantPhysique = montantAutomatique (ecart = 0). */
    public void produireRapportAutomatique(Inventaire inventaire, BilanInventaireRequest request) {
        Magasin magasin = inventaire.getMagasin();
        LocalDate dateFinPeriode = LocalDate.now();
        BigDecimal montantAutomatique = stockService.computeValuation(magasin).valeurTotale();
        BigDecimal depense = computeDepense(magasin, request.dateDebutPeriode(), dateFinPeriode);

        RapportInventaireCommand command = new RapportInventaireCommand(
                montantAutomatique, montantAutomatique,
                request.montantCaisse(), depense,
                request.montantRoulement(), request.dateDebutPeriode(), dateFinPeriode
        );
        rapportInventaireService.create(inventaire, command);
    }

    /** Construit la commande de rapport (valorisation lignes + depenses periode + saisies caisse/roulement) et delegue la creation au domain service. */
    public void produireRapport(Inventaire inventaire,
                                List<LigneInventaire> lignes,
                                BilanInventaireRequest request) {
        Magasin magasin = inventaire.getMagasin();
        LocalDate dateFinPeriode = LocalDate.now();
        BigDecimal montantAutomatique = computeMontantStock(lignes, true);
        BigDecimal montantPhysique = computeMontantStock(lignes, false);
        BigDecimal depense = computeDepense(magasin, request.dateDebutPeriode(), dateFinPeriode);

        RapportInventaireCommand command = new RapportInventaireCommand(
                montantAutomatique, montantPhysique, request.montantCaisse(), depense,
                request.montantRoulement(), request.dateDebutPeriode(), dateFinPeriode
        );
        rapportInventaireService.create(inventaire, command);
    }

    /** Valorise les lignes au prix saisi par l'utilisateur (fallback : prixAchat PF). */
    public BigDecimal computeMontantStock(List<LigneInventaire> lignes, boolean theorique) {
        return lignes.stream()
                .map(ligne -> {
                    int qte = theorique ? ligne.getQuantiteTheorique() : ligne.getQuantiteReelle();
                    BigDecimal prix = ligne.getPrixUnitaire() != null
                            ? ligne.getPrixUnitaire()
                            : ligne.getProductFournisseur().getPrixAchat();
                    return prix.multiply(BigDecimal.valueOf(qte));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Agregre les depenses du magasin sur la periode [dateDebut, dateFin]. */
    public BigDecimal computeDepense(Magasin magasin, LocalDate dateDebut, LocalDate dateFin) {
        DepenseFilter filter = new DepenseFilter(
                magasin.getId(), null, null, null, DateHelper.format(dateDebut), DateHelper.format(dateFin), 0, 1
        );
        DepenseTotalResponse total = depenseService.computeTotal(filter);
        return total != null && total.montantTotal() != null ? total.montantTotal() : BigDecimal.ZERO;
    }
}
