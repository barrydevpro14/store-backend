package org.store.stock.application.service;

import org.store.stock.application.dto.MarginReportFilter;
import org.store.stock.application.dto.MarginReportResponse;

public interface IMarginReportService {

    /**
     * Calcule le total des marges réelles sur les SortieStock du magasin ciblé,
     * filtrable par produit, fournisseur et période. Retourne la marge totale,
     * la quantité vendue totale et le nombre de sorties consommées.
     */
    MarginReportResponse compute(MarginReportFilter marginReportFilter);
}
