package org.store.reporting.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.IPdfService;
import org.store.common.service.ValidatorService;
import org.store.depense.application.dto.DepenseTotalResponse;
import org.store.depense.application.service.IDepenseService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.application.service.IPdfFormatSettingService;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.reporting.application.dto.MagasinOverviewStatsResponse;
import org.store.reporting.application.dto.MagasinVentesStatsResponse;
import org.store.reporting.application.dto.ReportPdfDocument;
import org.store.reporting.application.dto.ReportPdfFilter;
import org.store.reporting.application.dto.ReportPdfLine;
import org.store.reporting.application.dto.ReportPdfSection;
import org.store.reporting.application.pdf.strategy.ReportPdfStrategyResolver;
import org.store.reporting.application.service.IMagasinReportingService;
import org.store.reporting.application.service.IReportPdfService;

import java.util.List;
import java.util.UUID;

/**
 * Builds the printable version of the reporting pages (Reporting, Ventes, Dépenses) and dispatches it to the format strategy.
 * Data comes from the existing reporting services, so the PDF always matches the KPIs shown on screen.
 */
@Service
@Transactional(readOnly = true)
public class ReportPdfServiceImpl implements IReportPdfService {

    private final IMagasinReportingService magasinReportingService;
    private final IDepenseService depenseService;
    private final IMagasinService magasinService;
    private final IPdfFormatSettingService pdfFormatSettingService;
    private final ReportPdfStrategyResolver strategyResolver;
    private final IPdfService pdf;
    private final ValidatorService validatorService;

    public ReportPdfServiceImpl(IMagasinReportingService magasinReportingService,
                                IDepenseService depenseService,
                                IMagasinService magasinService,
                                IPdfFormatSettingService pdfFormatSettingService,
                                ReportPdfStrategyResolver strategyResolver,
                                IPdfService pdf,
                                ValidatorService validatorService) {
        this.magasinReportingService = magasinReportingService;
        this.depenseService = depenseService;
        this.magasinService = magasinService;
        this.pdfFormatSettingService = pdfFormatSettingService;
        this.strategyResolver = strategyResolver;
        this.pdf = pdf;
        this.validatorService = validatorService;
    }

    /** Generates the PDF of the Reporting page KPIs for the chosen magasin and period. */
    @Override
    public byte[] generateOverview(ReportPdfFilter reportPdfFilter) {
        Magasin magasin = resolveAccessibleMagasin(reportPdfFilter);

        MagasinOverviewStatsResponse stats = magasinReportingService.getOverview(reportPdfFilter.toMagasinOverviewFilter());

        List<ReportPdfLine> kpis = List.of(
                new ReportPdfLine(pdf.msg("pdf.report.kpi.chiffreAffaires"), pdf.formatAmount(stats.montantTotalCommandeVentes())),
                new ReportPdfLine(pdf.msg("pdf.report.kpi.paiementsEncaisses"), pdf.formatAmount(stats.totalPaiementVentes())),
                new ReportPdfLine(pdf.msg("pdf.report.kpi.nombreVentes"), String.valueOf(stats.nombreCommandeVentes())),
                new ReportPdfLine(pdf.msg("pdf.report.kpi.ticketMoyen"), pdf.formatAmount(stats.ticketMoyen())),
                new ReportPdfLine(pdf.msg("pdf.report.kpi.facturesVenteImpayees"), String.valueOf(stats.facturesVenteImpayees())),
                new ReportPdfLine(pdf.msg("pdf.report.kpi.achatsEnAttente"), String.valueOf(stats.achatsEnAttente())),
                new ReportPdfLine(pdf.msg("pdf.report.kpi.facturesAchatImpayees"), String.valueOf(stats.facturesAchatImpayees()))
        );

        ReportPdfDocument reportPdfDocument = new ReportPdfDocument(
                pdf.msg("pdf.report.overview.title"),
                reportPdfFilter.periodLabel(),
                List.of(new ReportPdfSection(null, pdf.msg("pdf.report.table.indicateur"), pdf.msg("pdf.report.table.valeur"), kpis))
        );

        return render(reportPdfDocument, magasin, reportPdfFilter.configId());
    }

    /** Generates the PDF of the sales reporting KPIs for the chosen magasin and period. */
    @Override
    public byte[] generateVentes(ReportPdfFilter reportPdfFilter) {
        Magasin magasin = resolveAccessibleMagasin(reportPdfFilter);

        MagasinVentesStatsResponse stats = magasinReportingService.getVentesStats(reportPdfFilter.toMagasinOverviewFilter());

        List<ReportPdfLine> kpis = List.of(
                new ReportPdfLine(pdf.msg("pdf.report.kpi.chiffreAffaires"), pdf.formatAmount(stats.montantTotalCommandeVentes())),
                new ReportPdfLine(pdf.msg("pdf.report.kpi.paiementsEncaisses"), pdf.formatAmount(stats.totalPaiementVentes())),
                new ReportPdfLine(pdf.msg("pdf.report.kpi.nombreVentes"), String.valueOf(stats.nombreCommandeVentes())),
                new ReportPdfLine(pdf.msg("pdf.report.kpi.ticketMoyen"), pdf.formatAmount(stats.ticketMoyen())),
                new ReportPdfLine(pdf.msg("pdf.report.kpi.facturesVenteImpayees"), String.valueOf(stats.facturesVenteImpayees()))
        );

        ReportPdfDocument reportPdfDocument = new ReportPdfDocument(
                pdf.msg("pdf.report.ventes.title"),
                reportPdfFilter.periodLabel(),
                List.of(new ReportPdfSection(null, pdf.msg("pdf.report.table.indicateur"), pdf.msg("pdf.report.table.valeur"), kpis))
        );

        return render(reportPdfDocument, magasin, reportPdfFilter.configId());
    }

    /** Generates the PDF of the expense reporting KPIs plus the breakdown by category. */
    @Override
    public byte[] generateDepenses(ReportPdfFilter reportPdfFilter) {
        Magasin magasin = resolveAccessibleMagasin(reportPdfFilter);

        DepenseTotalResponse total = depenseService.computeTotal(reportPdfFilter.toDepenseFilter());

        List<ReportPdfLine> kpis = List.of(
                new ReportPdfLine(pdf.msg("pdf.report.kpi.totalDepenses"), pdf.formatAmount(total.montantTotal())),
                new ReportPdfLine(pdf.msg("pdf.report.kpi.nombreDepenses"), String.valueOf(total.nombreDepenses()))
        );

        List<ReportPdfLine> categories = depenseService.computeByCategory(reportPdfFilter.toDepenseFilter()).stream()
                .map(categorie -> new ReportPdfLine(categorie.categoryNom(), pdf.formatAmount(categorie.montantTotal())))
                .toList();

        ReportPdfSection kpiSection = new ReportPdfSection(
                null, pdf.msg("pdf.report.table.indicateur"), pdf.msg("pdf.report.table.valeur"), kpis);
        ReportPdfSection categorySection = new ReportPdfSection(
                pdf.msg("pdf.report.depenses.byCategory"), pdf.msg("pdf.report.table.categorie"), pdf.msg("pdf.report.table.montant"), categories);

        ReportPdfDocument reportPdfDocument = new ReportPdfDocument(
                pdf.msg("pdf.report.depenses.title"),
                reportPdfFilter.periodLabel(),
                categories.isEmpty() ? List.of(kpiSection) : List.of(kpiSection, categorySection)
        );

        return render(reportPdfDocument, magasin, reportPdfFilter.configId());
    }

    /** Validates the filter and returns the magasin once the current user is allowed to access it. */
    @Override
    public Magasin resolveAccessibleMagasin(ReportPdfFilter reportPdfFilter) {
        validatorService.validate(reportPdfFilter);
        return magasinService.ensureAccessibleByCurrentUser(magasinService.findById(reportPdfFilter.magasinId()));
    }

    /** Renders a report document in the PDF format identified by configId, with the magasin's printer settings. */
    @Override
    public byte[] render(ReportPdfDocument reportPdfDocument, Magasin magasin, UUID configId) {
        PdfFormatConfig config = pdfFormatSettingService.resolveEffectiveConfig(configId, magasin.getId());
        return strategyResolver.resolve(config.getFormat()).generate(reportPdfDocument, magasin, config);
    }
}
