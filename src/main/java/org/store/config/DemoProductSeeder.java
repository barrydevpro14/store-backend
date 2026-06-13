package org.store.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.dto.FournisseurRequest;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.service.FournisseurDomainService;
import org.store.magasin.domain.model.Magasin;
import org.store.magasin.domain.service.MagasinDomainService;
import org.store.produit.application.dto.CategoryProductRequest;
import org.store.produit.application.dto.ProductFournisseurCreate;
import org.store.produit.application.dto.ProductFournisseurRequest;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.dto.QualityRequest;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.model.Quality;
import org.store.produit.domain.service.CategoryProductDomainService;
import org.store.produit.domain.service.ProductDomainService;
import org.store.produit.domain.service.ProductFournisseurDomainService;
import org.store.produit.domain.service.QualityDomainService;
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.StockDomainService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Seeds 20 demo products (auto parts) with stock into the first available magasin.
 * Runs only when {@code security.rbac.sync=true} and the magasin has no products yet.
 * Idempotent: guarded by checking for the "Auto Pièces Pro" fournisseur.
 */
@Profile("dev")
@Component
@Order(2)
public class DemoProductSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoProductSeeder.class);
    private static final String SEED_FOURNISSEUR_NOM = "Auto Pièces Pro";

    private final MagasinDomainService magasinDomainService;
    private final CategoryProductDomainService categoryProductDomainService;
    private final QualityDomainService qualityDomainService;
    private final FournisseurDomainService fournisseurDomainService;
    private final ProductDomainService productDomainService;
    private final ProductFournisseurDomainService productFournisseurDomainService;
    private final EntreeStockDomainService entreeStockDomainService;
    private final StockDomainService stockDomainService;

    public DemoProductSeeder(MagasinDomainService magasinDomainService,
                             CategoryProductDomainService categoryProductDomainService,
                             QualityDomainService qualityDomainService,
                             FournisseurDomainService fournisseurDomainService,
                             ProductDomainService productDomainService,
                             ProductFournisseurDomainService productFournisseurDomainService,
                             EntreeStockDomainService entreeStockDomainService,
                             StockDomainService stockDomainService) {
        this.magasinDomainService = magasinDomainService;
        this.categoryProductDomainService = categoryProductDomainService;
        this.qualityDomainService = qualityDomainService;
        this.fournisseurDomainService = fournisseurDomainService;
        this.productDomainService = productDomainService;
        this.productFournisseurDomainService = productFournisseurDomainService;
        this.entreeStockDomainService = entreeStockDomainService;
        this.stockDomainService = stockDomainService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    @Transactional
    public void seed() {
        List<Magasin> magasins = magasinDomainService.findAll();
        if (magasins.isEmpty()) {
            log.info("DemoProductSeeder: no magasin found — skipping");
            return;
        }

        Magasin magasin = magasins.get(0);
        var entreprise = magasin.getEntreprise();

        boolean alreadySeeded = fournisseurDomainService.findAll().stream()
                .anyMatch(f -> SEED_FOURNISSEUR_NOM.equals(f.getNom())
                        && entreprise.getId().equals(f.getEntreprise().getId()));
        if (alreadySeeded) {
            log.info("DemoProductSeeder: already seeded for magasin '{}' — skipping", magasin.getNom());
            return;
        }

        log.info("DemoProductSeeder: seeding 20 demo products into magasin '{}'", magasin.getNom());

        // ── Categories ────────────────────────────────────────────────────────
        CategoryProduct catMoteurs   = categoryProductDomainService.create(new CategoryProductRequest("Moteurs",      "Moteurs et pièces moteur"),  entreprise);
        CategoryProduct catFreinage  = categoryProductDomainService.create(new CategoryProductRequest("Freinage",     "Systèmes de freinage"),       entreprise);
        CategoryProduct catElec      = categoryProductDomainService.create(new CategoryProductRequest("Électricité",  "Pièces électriques"),         entreprise);
        CategoryProduct catTransmiss = categoryProductDomainService.create(new CategoryProductRequest("Transmission", "Boîte de vitesses et embrayage"), entreprise);
        CategoryProduct catCarross   = categoryProductDomainService.create(new CategoryProductRequest("Carrosserie",  "Pièces de carrosserie"),      entreprise);

        // ── Qualities ─────────────────────────────────────────────────────────
        Quality qNeuf    = qualityDomainService.create(new QualityRequest("Neuf",       "Article neuf d'origine"),     entreprise);
        Quality qOccas   = qualityDomainService.create(new QualityRequest("Occasion",   "Article d'occasion en bon état"), entreprise);
        Quality qRecondi = qualityDomainService.create(new QualityRequest("Reconditionné", "Article remis à neuf"),    entreprise);

        // ── Fournisseur ───────────────────────────────────────────────────────
        Fournisseur fournisseur = fournisseurDomainService.create(
                new FournisseurRequest(SEED_FOURNISSEUR_NOM, null, null, null, "Zone industrielle, Dakar", "APR-001", "Sénégal"),
                entreprise);

        // ── Products + PF + Stock ─────────────────────────────────────────────
        record Def(String nom, String ref, CategoryProduct cat, Quality quality, int prixAchat, int prixVente, int stock) {}

        List<Def> defs = List.of(
            new Def("Moteur Camion",              "MOT-CAM",  catMoteurs,   qOccas,   250000, 320000, 3),
            new Def("Moteur Voiture",             "MOT-VOI",  catMoteurs,   qNeuf,    180000, 230000, 4),
            new Def("Radiateur",                  "RAD-001",  catMoteurs,   qNeuf,     45000,  62000, 8),
            new Def("Pompe à eau",                "PMP-EAU",  catMoteurs,   qNeuf,     18000,  25000, 6),
            new Def("Courroie de distribution",   "COU-DIS",  catMoteurs,   qNeuf,      8500,  13000, 12),
            new Def("Disque de frein avant",      "DIS-FRE",  catFreinage,  qNeuf,     22000,  30000, 10),
            new Def("Plaquettes de frein",        "PLA-FRE",  catFreinage,  qNeuf,      9000,  14000, 20),
            new Def("Maître-cylindre",            "MCY-001",  catFreinage,  qNeuf,     15000,  22000, 5),
            new Def("Amortisseur avant gauche",   "AMO-AVG",  catFreinage,  qNeuf,     35000,  48000, 6),
            new Def("Amortisseur avant droit",    "AMO-AVD",  catFreinage,  qNeuf,     35000,  48000, 6),
            new Def("Alternateur",                "ALT-001",  catElec,      qNeuf,     55000,  75000, 5),
            new Def("Démarreur",                  "DEM-001",  catElec,      qOccas,    28000,  40000, 4),
            new Def("Batterie 12V 45Ah",          "BAT-45",   catElec,      qNeuf,     42000,  58000, 8),
            new Def("Bougie d'allumage",          "BOU-ALL",  catElec,      qNeuf,      2500,   4000, 30),
            new Def("Moteur Camion",              "MOT-CAM",  catMoteurs,   qNeuf,    380000, 480000, 2),
            new Def("Embrayage complet",          "EMB-001",  catTransmiss, qNeuf,     65000,  88000, 4),
            new Def("Boîte de vitesses manuelle", "BVT-MAN",  catTransmiss, qOccas,   120000, 165000, 2),
            new Def("Carburateur",                "CAR-001",  catTransmiss, qRecondi,  32000,  45000, 3),
            new Def("Pare-chocs avant",           "PAR-AV",   catCarross,   qNeuf,     48000,  68000, 4),
            new Def("Pot d'échappement",          "POT-ECH",  catCarross,   qNeuf,     25000,  36000, 6)
        );

        for (Def d : defs) {
            createProductWithStock(magasin, entreprise, fournisseur, d.cat(), d.quality(),
                    d.nom(), d.ref(), d.prixAchat(), d.prixVente(), d.stock());
        }

        log.info("DemoProductSeeder: {} products seeded into '{}'", defs.size(), magasin.getNom());
    }

    private void createProductWithStock(Magasin magasin, org.store.entreprise.domain.model.Entreprise entreprise,
                                        Fournisseur fournisseur, CategoryProduct category, Quality quality,
                                        String nom, String reference,
                                        int prixAchat, int prixVente, int quantite) {
        Product product = productDomainService.findByReferenceAndEntrepriseId(reference, entreprise.getId())
                .orElseGet(() -> productDomainService.create(
                        new ProductRequest(nom, reference, null, category.getId()),
                        category, entreprise));

        ProductFournisseur pf = productFournisseurDomainService.create(new ProductFournisseurCreate(
                new ProductFournisseurRequest(product.getId(), fournisseur.getId(),
                        quality.getId(), BigDecimal.valueOf(prixAchat), BigDecimal.valueOf(prixVente), null, null),
                product, fournisseur, quality));

        entreeStockDomainService.create(new EntreeStockCreate(
                magasin, product, pf, quantite, BigDecimal.valueOf(prixAchat),
                null, null, null));

        stockDomainService.createOrUpdateEntry(new StockEntryContext(
                magasin, pf, quantite, BigDecimal.valueOf(prixAchat)));
    }
}
