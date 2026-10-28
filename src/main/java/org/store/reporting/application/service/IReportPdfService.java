package org.store.reporting.application.service;

import org.store.magasin.domain.model.Magasin;
import org.store.reporting.application.dto.ReportPdfDocument;
import org.store.reporting.application.dto.ReportPdfFilter;

import java.util.UUID;

public interface IReportPdfService {

    /** Generates the PDF of the Reporting page KPIs for the chosen magasin and period. */
    byte[] generateOverview(ReportPdfFilter reportPdfFilter);

    /** Generates the PDF of the sales reporting KPIs for the chosen magasin and period. */
    byte[] generateVentes(ReportPdfFilter reportPdfFilter);

    /** Generates the PDF of the expense reporting KPIs plus the breakdown by category. */
    byte[] generateDepenses(ReportPdfFilter reportPdfFilter);

    /** Validates the filter and returns the magasin once the current user is allowed to access it. */
    Magasin resolveAccessibleMagasin(ReportPdfFilter reportPdfFilter);

    /** Renders a report document in the PDF format identified by configId, with the magasin's printer settings. */
    byte[] render(ReportPdfDocument reportPdfDocument, Magasin magasin, UUID configId);
}
