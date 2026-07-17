package org.store.stock.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ExcelEntreeStockRow;
import org.store.common.dto.ExcelStockParseResult;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IExcelEntreeStockRowService;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.dto.QualityRequest;
import org.store.produit.application.service.ICategoryProductService;
import org.store.produit.application.service.IProductService;
import org.store.produit.application.service.IQualityService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.Quality;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.LigneEntreeStockRequest;
import org.store.stock.application.dto.StockImportError;
import org.store.stock.application.dto.StockImportResult;
import org.store.stock.application.service.IEntreeStockService;
import org.store.stock.application.service.IStockImportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestre l'import d'entrées stock depuis un fichier Excel.
 * Phase 1 : parse structurel (colonnes manquantes, formats invalides) via {@link IExcelEntreeStockRowService}.
 * Phase 2 : résolution métier par ligne — catégorie et qualité créées si absentes, produit créé si absent.
 * Les lignes valides sont regroupées en un seul {@link EntreeStockRequest} et committées en un batch.
 * Les lignes en erreur sont rapportées dans {@link StockImportResult#erreurs()} sans bloquer les lignes valides.
 */
@Service
public class StockImportServiceImpl implements IStockImportService {

    private record ImportContext(List<LigneEntreeStockRequest> validLignes, List<StockImportError> errors) {}

    private final IExcelEntreeStockRowService excelParser;
    private final IEntreeStockService entreeStockService;
    private final ICategoryProductService categoryProductService;
    private final IProductService productService;
    private final IQualityService qualityService;
    private final IMessageSourceService messageSourceService;

    public StockImportServiceImpl(IExcelEntreeStockRowService excelParser,
                                  IEntreeStockService entreeStockService,
                                  ICategoryProductService categoryProductService,
                                  IProductService productService,
                                  IQualityService qualityService,
                                  IMessageSourceService messageSourceService) {
        this.excelParser = excelParser;
        this.entreeStockService = entreeStockService;
        this.categoryProductService = categoryProductService;
        this.productService = productService;
        this.qualityService = qualityService;
        this.messageSourceService = messageSourceService;
    }

    @Override
    public StockImportResult importFromFile(MultipartFile file, UUID magasinId, UUID fournisseurId) {
        ExcelStockParseResult parseResult = excelParser.parseRows(file);

        List<StockImportError> errors = new ArrayList<>(toImportErrors(parseResult.errors()));
        List<LigneEntreeStockRequest> validLignes = new ArrayList<>();
        ImportContext context = new ImportContext(validLignes, errors);

        parseResult.rows().forEach(row -> processRow(row, context));

        if (!validLignes.isEmpty()) {
            entreeStockService.create(new EntreeStockRequest(magasinId, fournisseurId, validLignes));
        }

        return new StockImportResult(validLignes.size(), errors.size(), errors);
    }

    /**
     * Résout ou crée le produit et la qualité d'une ligne parsée, valide la marge,
     * puis ajoute une {@link LigneEntreeStockRequest} au contexte ou enregistre une erreur.
     */
    private void processRow(ExcelEntreeStockRow row, ImportContext context) {
        UUID productId = resolveOrCreateProduct(row);

        Optional<Quality> qualityOpt = qualityService.findByLibelle(row.qualite());
        UUID qualityId = qualityOpt.isPresent()
                ? qualityOpt.get().getId()
                : qualityService.create(new QualityRequest(row.qualite(), null)).id();

        BigDecimal prixAchat = new BigDecimal(row.prixAchat());
        BigDecimal prixVente = new BigDecimal(row.prixVente());

        if (prixVente.compareTo(prixAchat) <= 0) {
            context.errors().add(new StockImportError(
                    row.referenceProduit(), row.nomProduit(),
                    messageSourceService.getMessage("excel.stock.import.margin.invalid",
                            new Object[]{row.lineNumber()})));
            return;
        }

        LocalDate dateExpiration = row.dateExpiration() != null ? LocalDate.parse(row.dateExpiration()) : null;

        context.validLignes().add(new LigneEntreeStockRequest(
                productId,
                qualityId,
                Integer.parseInt(row.quantite()),
                prixAchat,
                prixVente,
                row.numeroLot(),
                dateExpiration
        ));
    }

    private UUID resolveOrCreateProduct(ExcelEntreeStockRow row) {
        Optional<Product> productOpt = productService.findByReferenceAndNom(row.referenceProduit(), row.nomProduit());

        if (productOpt.isPresent()) {
            return productOpt.get().getId();
        }

        UUID categoryId = categoryProductService.findOrCreateByLibelle(row.categorie()).getId();

        return productService.create(new ProductRequest(
                row.nomProduit(), row.referenceProduit(), null, categoryId)).id();
    }

    private List<StockImportError> toImportErrors(List<String> parseErrors) {
        return parseErrors.stream()
                .map(msg -> new StockImportError(null, null, msg))
                .toList();
    }
}
