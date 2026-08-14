REFONTE PDF — PLAN D'IMPLÉMENTATION
=====================================

Décisions validées
------------------

- Formats supportés : A4, A5, THERMAL_80MM, THERMAL_58MM (CUSTOM reporté)
- Le format ne dépend PAS du magasin
- Le controller reçoit un UUID de `pdf_format_config` (pas l'enum directement)
- Aucune valeur par défaut sur le paramètre `configId` — le frontend choisit
- Seul le header change par rapport au layout actuel (tableau lignes/totaux/paiements conservé)
- Architecture Option B : AbstractPdfRenderer partagé + deux hiérarchies de strategy
  (une pour FactureClient, une pour BonCommandeAchat)
- Strategy Pattern pour éviter tout if/else sur le format


Nouveau modèle de header (model_facture.jpeg)
----------------------------------------------

+------------------------------------------+
| [LOGO]   NOM MAGASIN (grand, centré)     |
|          Activité / secteur              |
|          Adresse — Ville                 |
|          Tel — Email                     |
+------------------------------------------+
| FACTURE / NOTE DE PRIX / BON COMMANDE    |  ← label document, gras, à gauche
+--------+----------+--------+-------------+
| NUMERO |   DATE   |  HEURE | COLLABORATEUR|
+--------+----------+--------+-------------+
| CLIENT : [nom complet]                   |
+------------------------------------------+

Le tableau des lignes, totaux, paiements et footer restent INCHANGÉS.


Flux d'appel
-------------

  Utilisateur choisit un format dans le frontend
         |
         | GET /api/v1/pdf-configs  →  liste des configs disponibles (id, label, format)
         |
  Utilisateur clique Télécharger
         |
         | GET /factures/{factureId}/pdf?configId={uuid}
         v
  Controller
         |
         v
  InvoicePdfService.generate(factureId, configId)
         |
         | 1. factureClientService.findById(factureId)
         | 2. pdfFormatConfigService.findById(configId)  ← lit l'enum depuis la ligne DB
         | 3. strategyResolver.resolve(config.getFormat())
         | 4. strategy.generate(facture, magasin, config)
         v
  PDF bytes


Même flux pour BonCommandeAchat :

  GET /commandes-achat/{commandeId}/bon-commande?configId={uuid}
         |
         v
  BonCommandePdfService.generate(commandeId, configId)


ÉTAPE 1 — PdfFormat enum + entité + migration + seed ✅
---------------------------------------------------------

Enum :

  public enum PdfFormat {
      A4,
      A5,
      THERMAL_80MM,
      THERMAL_58MM
  }


Entité PdfFormatConfig (table : pdf_format_config) :

  id              UUID PK
  code            VARCHAR(50) UNIQUE NOT NULL   ← ex : "A4"
  label           VARCHAR(100) NOT NULL          ← ex : "Facture A4"
  format          VARCHAR(50) NOT NULL           ← enum PdfFormat (CHECK constraint)
  page_width      DECIMAL(10,2)
  page_height     DECIMAL(10,2)                  ← 0 = hauteur dynamique (thermique)
  margin_left     DECIMAL(10,2) DEFAULT 40
  margin_right    DECIMAL(10,2) DEFAULT 40
  margin_top      DECIMAL(10,2) DEFAULT 40
  margin_bottom   DECIMAL(10,2) DEFAULT 40
  font_size_title DECIMAL(5,2)
  font_size_normal DECIMAL(5,2)
  font_size_small DECIMAL(5,2)
  enabled         BOOLEAN NOT NULL DEFAULT TRUE
  created_at      TIMESTAMP NOT NULL
  updated_at      TIMESTAMP


Seed (DataInitializer ou migration Flyway) :

  A4
    code=A4, label=Facture A4, format=A4
    page_width=595, page_height=842
    margin_left=40, margin_right=40, margin_top=40, margin_bottom=120
    font_size_title=14, font_size_normal=10, font_size_small=8

  A5
    code=A5, label=Facture A5, format=A5
    page_width=420, page_height=595
    margin_left=25, margin_right=25, margin_top=25, margin_bottom=80
    font_size_title=12, font_size_normal=9, font_size_small=7

  THERMAL_80MM
    code=THERMAL_80MM, label=Ticket thermique 80mm, format=THERMAL_80MM
    page_width=226, page_height=0
    margin_left=10, margin_right=10, margin_top=10, margin_bottom=10
    font_size_title=10, font_size_normal=8, font_size_small=7

  THERMAL_58MM
    code=THERMAL_58MM, label=Ticket thermique 58mm, format=THERMAL_58MM
    page_width=164, page_height=0
    margin_left=8, margin_right=8, margin_top=8, margin_bottom=8
    font_size_title=9, font_size_normal=7, font_size_small=6


ÉTAPE 2 — IPdfFormatConfigService ✅
--------------------------------------

  PdfFormatConfig findById(UUID id)   ← lève EntityException si not found ou !enabled
  List<PdfFormatConfigResponse> findAll()  ← GET /api/v1/pdf-configs (enabled=true)


ÉTAPE 3 — AbstractPdfRenderer ✅
----------------------------------

Classe abstraite partagée entre les deux documents.

  public abstract class AbstractPdfRenderer {

      // NOUVEAU header (modèle validé)
      protected void addHeader(Document doc, Magasin magasin, String numeroDoc,
                               LocalDate dateDoc, LocalTime heureDoc,
                               String collaborateur, String clientLabel,
                               String documentLabel, PdfFormatConfig config) { ... }

      // INCHANGÉS — réutilisés tels quels
      protected void addLinesTable(Document doc, ..., PdfFormatConfig config) { ... }
      protected void addTotalsAndPayments(Document doc, ..., PdfFormatConfig config) { ... }
      protected void configureFooter(PdfWriter writer, Magasin magasin) { ... }
  }


ÉTAPE 4 — Structures de packages ✅
-------------------------------------

  vente/application/pdf/
  ├── strategy/
  │   ├── InvoicePdfStrategy.java               interface
  │   ├── A4InvoicePdfStrategy.java
  │   ├── A5InvoicePdfStrategy.java
  │   ├── Thermal80InvoicePdfStrategy.java
  │   ├── Thermal58InvoicePdfStrategy.java
  │   └── InvoicePdfStrategyResolver.java
  └── renderer/
      ├── AbstractPdfRenderer.java              (partagé avec achat)
      ├── StandardInvoicePdfRenderer.java       (A4 + A5)
      └── ThermalInvoicePdfRenderer.java

  achat/application/pdf/
  ├── strategy/
  │   ├── BonCommandePdfStrategy.java           interface
  │   ├── A4BonCommandePdfStrategy.java
  │   ├── A5BonCommandePdfStrategy.java
  │   ├── Thermal80BonCommandePdfStrategy.java
  │   ├── Thermal58BonCommandePdfStrategy.java
  │   └── BonCommandePdfStrategyResolver.java
  └── renderer/
      ├── StandardBonCommandePdfRenderer.java   (A4 + A5)
      └── ThermalBonCommandePdfRenderer.java

  common/pdf/
  └── renderer/
      └── AbstractPdfRenderer.java


ÉTAPE 5 — Interfaces strategy ✅
----------------------------------

  // Facture vente
  public interface InvoicePdfStrategy {
      PdfFormat supports();
      byte[] generate(FactureClient facture, Magasin magasin, PdfFormatConfig config);
  }

  // Bon commande achat
  public interface BonCommandePdfStrategy {
      PdfFormat supports();
      byte[] generate(CommandeAchat commande, Magasin magasin, PdfFormatConfig config);
  }


ÉTAPE 6 — Resolvers (identiques, types différents) ✅
-------------------------------------------------------

  @Component
  public class InvoicePdfStrategyResolver {

      private final Map<PdfFormat, InvoicePdfStrategy> strategies;

      public InvoicePdfStrategyResolver(List<InvoicePdfStrategy> strategies) {
          this.strategies = strategies.stream()
                  .collect(Collectors.toMap(
                          InvoicePdfStrategy::supports,
                          Function.identity()
                  ));
      }

      public InvoicePdfStrategy resolve(PdfFormat format) {
          InvoicePdfStrategy strategy = strategies.get(format);
          if (strategy == null) {
              throw new BadArgumentException("pdf.format.unsupported");
          }
          return strategy;
      }
  }


ÉTAPE 7 — Services ✅
-----------------------

  // Signature actuelle
  public byte[] generate(UUID factureId)

  // Nouvelle signature
  public byte[] generate(UUID factureId, UUID configId) {
      FactureClient facture = factureClientService.findById(factureId);
      // ownership check
      PdfFormatConfig config = pdfFormatConfigService.findById(configId);
      Magasin magasin = facture.getCommande().getMagasin();
      InvoicePdfStrategy strategy = strategyResolver.resolve(config.getFormat());
      return strategy.generate(facture, magasin, config);
  }

  // Idem pour BonCommandePdfService
  public byte[] generate(UUID commandeId, UUID configId) { ... }


ÉTAPE 8 — Controllers ✅
--------------------------

  // Facture vente
  @GetMapping("/{factureId}/pdf")
  public ResponseEntity<byte[]> downloadPdf(
          @PathVariable UUID factureId,
          @RequestParam UUID configId          // pas de defaultValue
  ) { ... }

  // Bon commande achat
  @GetMapping("/{commandeId}/bon-commande")
  public ResponseEntity<byte[]> downloadBonCommande(
          @PathVariable UUID commandeId,
          @RequestParam UUID configId          // pas de defaultValue
  ) { ... }


ÉTAPE 9 — Endpoint public configs ✅
--------------------------------------

  GET /api/v1/pdf-configs

  Retourne : List<PdfFormatConfigResponse(id, code, label, format)>
  Permission : tout utilisateur authentifié
  Filtre : enabled = true


ÉTAPE 10 — Frontend ✅
---------------------

  1. Au chargement de la page (ou du dialog de download) :
     useQuery → GET /api/v1/pdf-configs → liste des formats disponibles

  2. Sélecteur (Select ou RadioGroup) : label affiché = config.label

  3. Bouton Télécharger :
     GET /factures/{factureId}/pdf?configId={config.id}
     GET /commandes-achat/{commandeId}/bon-commande?configId={config.id}


Règle d'architecture à conserver
----------------------------------

  Le Service métier ne connaît PAS le layout.
  Le Controller ne connaît PAS le layout.
  Le Resolver ne connaît PAS le layout.
  La Strategy connaît le type de document (FactureClient ou CommandeAchat).
  Le Renderer connaît la mise en page.
  PdfFormatConfig connaît les paramètres physiques du format.
