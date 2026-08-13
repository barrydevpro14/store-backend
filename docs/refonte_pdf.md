ARCHITECTURE PDF MULTI-FORMATS
==============================

Objectif
--------

Le PDF de facture/bon commande doit pouvoir être généré dans plusieurs formats :

- A4
- A5
- THERMAL_80MM
- THERMAL_58MM
- CUSTOM

Le format est choisi par l'utilisateur au moment du téléchargement.

Le format ne dépend PAS du magasin.

On utilise le Strategy Pattern afin d'éviter d'avoir du code du type :

    if (format == A4) {
        ...
    } else if (format == A5) {
        ...
    } else if (format == THERMAL) {
        ...
    }

Chaque format possède sa propre stratégie de rendu.


1. Modèle de configuration
--------------------------

Créer une table de configuration indépendante du magasin.

Exemple :

CREATE TABLE pdf_format_config (
id UUID PRIMARY KEY,
code VARCHAR(50) NOT NULL UNIQUE,
label VARCHAR(100) NOT NULL,

    page_width DECIMAL(10,2),
    page_height DECIMAL(10,2),

    margin_left DECIMAL(10,2) DEFAULT 40,
    margin_right DECIMAL(10,2) DEFAULT 40,
    margin_top DECIMAL(10,2) DEFAULT 40,
    margin_bottom DECIMAL(10,2) DEFAULT 40,

    font_size_title DECIMAL(5,2),
    font_size_normal DECIMAL(5,2),
    font_size_small DECIMAL(5,2),

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);


Exemples de configurations :

A4
--
code          = A4
label         = Facture A4
page_width    = 595
page_height   = 842

A5
--
code          = A5
label         = Facture A5
page_width    = 420
page_height   = 595

THERMAL_80MM
------------
code          = THERMAL_80MM
label         = Ticket thermique 80mm
page_width    = 226
page_height   = 0

THERMAL_58MM
------------
code          = THERMAL_58MM
label         = Ticket thermique 58mm
page_width    = 164
page_height   = 0


IMPORTANT :

Pour les imprimantes thermiques, la hauteur peut être dynamique.

Le document est donc construit avec une largeur fixe et une hauteur calculée
en fonction du contenu.


2. Enum du format
-----------------

public enum PdfFormat {

    A4,
    A5,
    THERMAL_80MM,
    THERMAL_58MM,
    CUSTOM
}


Le CUSTOM n'est pas obligatoire au début.

Il devient intéressant si l'administrateur doit pouvoir définir :

- largeur
- hauteur
- marges
- tailles de police
- etc.

Pour A4/A5/thermique, les configurations sont prédéfinies.


3. DTO de demande
-----------------

Le format est choisi lors du download.

public record PdfGenerationRequest(
PdfFormat format
) {
}


Exemple :

{
"format": "A4"
}


ou :

{
"format": "THERMAL_80MM"
}


4. Strategy Pattern
-------------------

Créer une interface commune :

public interface InvoicePdfStrategy {

    PdfFormat supports();

    byte[] generate(
            FactureClient facture,
            Magasin magasin,
            PdfColors colors
    );
}


Chaque format possède sa stratégie.


5. Strategy A4
--------------

@Component
public class A4InvoicePdfStrategy implements InvoicePdfStrategy {

    private final PdfService pdf;

    public A4InvoicePdfStrategy(PdfService pdf) {
        this.pdf = pdf;
    }

    @Override
    public PdfFormat supports() {
        return PdfFormat.A4;
    }

    @Override
    public byte[] generate(
            FactureClient facture,
            Magasin magasin,
            PdfColors colors
    ) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document doc = new Document(
                    PageSize.A4,
                    40,
                    40,
                    40,
                    120
            );

            PdfWriter writer = PdfWriter.getInstance(doc, out);

            pdf.configureFooter(writer, magasin);

            doc.open();

            pdf.addHeader(doc, magasin, facture, colors);

            doc.add(Chunk.NEWLINE);

            pdf.addClientAndMeta(doc, facture);

            doc.add(Chunk.NEWLINE);

            pdf.addLinesTable(doc, facture, colors);

            doc.add(Chunk.NEWLINE);

            pdf.addTotalsAndPayments(doc, facture, colors);

            doc.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new PdfGenerationException(
                    "Erreur génération PDF A4",
                    e
            );
        }
    }
}


6. Strategy A5
--------------

@Component
public class A5InvoicePdfStrategy implements InvoicePdfStrategy {

    private final PdfService pdf;

    public A5InvoicePdfStrategy(PdfService pdf) {
        this.pdf = pdf;
    }

    @Override
    public PdfFormat supports() {
        return PdfFormat.A5;
    }

    @Override
    public byte[] generate(
            FactureClient facture,
            Magasin magasin,
            PdfColors colors
    ) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document doc = new Document(
                    PageSize.A5,
                    25,
                    25,
                    25,
                    80
            );

            PdfWriter writer = PdfWriter.getInstance(doc, out);

            pdf.configureFooter(writer, magasin);

            doc.open();

            pdf.addHeader(doc, magasin, facture, colors);

            doc.add(Chunk.NEWLINE);

            pdf.addClientAndMeta(doc, facture);

            doc.add(Chunk.NEWLINE);

            pdf.addLinesTable(doc, facture, colors);

            doc.add(Chunk.NEWLINE);

            pdf.addTotalsAndPayments(doc, facture, colors);

            doc.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new PdfGenerationException(
                    "Erreur génération PDF A5",
                    e
            );
        }
    }
}


7. Strategy thermique
---------------------

Le thermique doit être traité différemment.

On ne doit PAS simplement faire :

    PageSize.A4 -> PageSize 80mm

car la structure du ticket est différente.

Le ticket doit généralement être :

- mono-colonne
- compact
- avec une police plus petite
- avec un header simplifié
- avec les lignes produit verticales
- avec les totaux simplifiés
- avec un footer compact


@Component
public class Thermal80InvoicePdfStrategy
implements InvoicePdfStrategy {

    private final ThermalInvoicePdfRenderer renderer;

    public Thermal80InvoicePdfStrategy(
            ThermalInvoicePdfRenderer renderer
    ) {
        this.renderer = renderer;
    }

    @Override
    public PdfFormat supports() {
        return PdfFormat.THERMAL_80MM;
    }

    @Override
    public byte[] generate(
            FactureClient facture,
            Magasin magasin,
            PdfColors colors
    ) {

        return renderer.render(
                facture,
                magasin,
                colors,
                226
        );
    }
}


8. Renderer thermique
---------------------

@Component
public class ThermalInvoicePdfRenderer {

    public byte[] render(
            FactureClient facture,
            Magasin magasin,
            PdfColors colors,
            float width
    ) {

        try (ByteArrayOutputStream out =
                     new ByteArrayOutputStream()) {

            Rectangle pageSize = new Rectangle(
                    width,
                    calculateHeight(facture)
            );

            Document doc = new Document(
                    pageSize,
                    10,
                    10,
                    10,
                    10
            );

            PdfWriter writer =
                    PdfWriter.getInstance(doc, out);

            doc.open();

            addThermalHeader(
                    doc,
                    magasin,
                    facture
            );

            addThermalClient(
                    doc,
                    facture
            );

            addThermalLines(
                    doc,
                    facture
            );

            addThermalTotals(
                    doc,
                    facture
            );

            addThermalFooter(
                    doc,
                    magasin
            );

            doc.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new PdfGenerationException(
                    "Erreur génération ticket thermique",
                    e
            );
        }
    }
}


9. Strategy Resolver
--------------------

Le service principal ne connaît pas les détails de A4/A5/thermique.

Il demande simplement au Resolver :

    "Donne-moi la stratégie correspondant au format."


@Component
public class InvoicePdfStrategyResolver {

    private final Map<PdfFormat, InvoicePdfStrategy> strategies;

    public InvoicePdfStrategyResolver(
            List<InvoicePdfStrategy> strategies
    ) {

        this.strategies = strategies.stream()
                .collect(Collectors.toMap(
                        InvoicePdfStrategy::supports,
                        Function.identity()
                ));
    }

    public InvoicePdfStrategy resolve(
            PdfFormat format
    ) {

        InvoicePdfStrategy strategy =
                strategies.get(format);

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Format PDF non supporté : " + format
            );
        }

        return strategy;
    }
}


10. Service principal
---------------------

Le service actuel :

public byte[] generate(UUID factureId)

devient :

public byte[] generate(
UUID factureId,
PdfFormat format
) {

    FactureClient facture =
            factureClientService.findById(factureId);

    OwnershipHelper.ensureOwnership(
            facture,
            facture.getCommande()
                    .getMagasin()
                    .getEntreprise()
                    .getId(),
            currentUserService
                    .getCurrent()
                    .entrepriseId(),
            "factureClient.notOwned"
    );

    Magasin magasin =
            facture.getCommande().getMagasin();

    PdfColors colors =
            resolveColors();

    InvoicePdfStrategy strategy =
            strategyResolver.resolve(format);

    return strategy.generate(
            facture,
            magasin,
            colors
    );
}


11. Controller
--------------

@GetMapping(
value = "/{factureId}/pdf",
produces = MediaType.APPLICATION_PDF_VALUE
)
public ResponseEntity<byte[]> downloadPdf(
@PathVariable UUID factureId,
@RequestParam(defaultValue = "A4")
PdfFormat format
) {

    byte[] pdf =
            invoicePdfService.generate(
                    factureId,
                    format
            );

    return ResponseEntity.ok()
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"facture-" +
                    factureId +
                    ".pdf\""
            )
            .contentType(
                    MediaType.APPLICATION_PDF
            )
            .body(pdf);
}


12. Utilisation
---------------

A4 :

GET /factures/{id}/pdf?format=A4


A5 :

GET /factures/{id}/pdf?format=A5


Thermique 80mm :

GET /factures/{id}/pdf?format=THERMAL_80MM


Thermique 58mm :

GET /factures/{id}/pdf?format=THERMAL_58MM


13. Pourquoi la table de configuration ?
----------------------------------------

La table permet de ne pas coder en dur toutes les propriétés du document.

Exemple :

PdfFormatConfig {

    code

    label

    pageWidth
    pageHeight

    marginLeft
    marginRight
    marginTop
    marginBottom

    fontSizeTitle
    fontSizeNormal
    fontSizeSmall

    enabled
}


Le Strategy récupère ensuite cette configuration.


14. Séparer Format et Layout
----------------------------

C'est un point IMPORTANT.

Il ne faut pas faire dépendre toute la logique de :

    PdfFormat.A4
    PdfFormat.A5
    PdfFormat.THERMAL


Le format définit principalement :

    dimensions
    marges
    typographie
    contraintes d'impression


Le Layout définit :

    Header
    Client
    Produits
    Totaux
    Paiements
    Footer


Donc on peut avoir :

InvoicePdfStrategy
|
+-- A4InvoicePdfStrategy
|
+-- A5InvoicePdfStrategy
|
+-- ThermalInvoicePdfStrategy
|
+-- Thermal80
+-- Thermal58


15. Encore mieux : Strategy + Renderer
--------------------------------------

Architecture recommandée :

InvoicePdfService
|
v
InvoicePdfStrategyResolver
|
+------------------+
|                  |
v                  v
A4Strategy             ThermalStrategy
|                  |
v                  v
A4Renderer              ThermalRenderer
|                  |
+--------+---------+
|
v
PdfFormatConfig


Le service métier ne contient donc aucune logique de mise en page.


16. Réutilisation du code
-------------------------

Les méthodes actuelles :

    addHeader(...)
    addClientAndMeta(...)
    addLinesTable(...)
    addTotalsAndPayments(...)

ne doivent pas toutes disparaître.

On peut les organiser dans un renderer commun :

public abstract class AbstractInvoicePdfRenderer {

    protected void addHeader(...) {
        ...
    }

    protected void addClientAndMeta(...) {
        ...
    }

    protected void addLinesTable(...) {
        ...
    }

    protected void addTotalsAndPayments(...) {
        ...
    }
}


Puis :

A4Renderer extends AbstractInvoicePdfRenderer

A5Renderer extends AbstractInvoicePdfRenderer

ThermalRenderer extends AbstractInvoicePdfRenderer


Mais attention :

Le thermique ayant une présentation très différente, il peut surcharger
certaines méthodes ou utiliser son propre renderer.


17. Structure finale des packages
---------------------------------

pdf/
|
+-- strategy/
|   |
|   +-- InvoicePdfStrategy.java
|   +-- A4InvoicePdfStrategy.java
|   +-- A5InvoicePdfStrategy.java
|   +-- Thermal80InvoicePdfStrategy.java
|   +-- Thermal58InvoicePdfStrategy.java
|   +-- InvoicePdfStrategyResolver.java
|
+-- renderer/
|   |
|   +-- AbstractInvoicePdfRenderer.java
|   +-- A4InvoicePdfRenderer.java
|   +-- A5InvoicePdfRenderer.java
|   +-- ThermalInvoicePdfRenderer.java
|
+-- config/
|   |
|   +-- PdfFormatConfig.java
|   +-- PdfFormatConfigRepository.java
|   +-- PdfFormatConfigService.java
|
+-- model/
|   |
|   +-- PdfFormat.java
|
+-- service/
|
+-- InvoicePdfService.java


18. Évolution future
--------------------

Cette architecture permet d'ajouter facilement :

    TICKET_80MM
    TICKET_58MM
    A3
    LETTER
    CUSTOM
    FACTURE_COMPTABLE
    BON_LIVRAISON
    DEVIS


sans modifier le service principal.

Par exemple :

@Component
public class A3InvoicePdfStrategy
implements InvoicePdfStrategy {

    @Override
    public PdfFormat supports() {
        return PdfFormat.A3;
    }

    ...
}


Le Resolver la détectera automatiquement.


19. CUSTOM
----------

CUSTOM devient utile si le besoin est :

    "L'utilisateur choisit lui-même les dimensions."

Exemple :

{
"format": "CUSTOM",
"width": 80,
"height": 200,
"unit": "MM"
}


Mais je recommande de NE PAS commencer avec CUSTOM.

Commencer avec :

    A4
    A5
    THERMAL_80MM
    THERMAL_58MM


Puis ajouter CUSTOM uniquement lorsque le besoin métier est confirmé.


20. Flux final
--------------

Utilisateur
|
| choisit "A4"
v
Frontend
|
| GET /factures/{id}/pdf?format=A4
v
Controller
|
v
InvoicePdfService
|
v
InvoicePdfStrategyResolver
|
| format = A4
v
A4InvoicePdfStrategy
|
v
A4InvoicePdfRenderer
|
v
PdfFormatConfig
|
v
PDF


Pour un ticket :

Utilisateur
|
| choisit "THERMAL_80MM"
v
Controller
|
v
InvoicePdfService
|
v
InvoicePdfStrategyResolver
|
v
Thermal80InvoicePdfStrategy
|
v
ThermalInvoicePdfRenderer
|
v
PDF thermique


21. Règle d'architecture
------------------------

Le principe à conserver est :

    Le Service métier ne connaît PAS le layout.

    Le Controller ne connaît PAS le layout.

    Le Resolver ne connaît PAS le layout.

    La Strategy connaît le type de document.

    Le Renderer connaît la mise en page.

    PdfFormatConfig connaît les paramètres physiques du format.


Cela permet d'avoir un code propre et surtout d'éviter que le service
de génération devienne un énorme :

    if / else if / else if