package org.store.plateforme.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.RevenuPeriodFilter;
import org.store.abonnement.application.service.IRevenuService;
import org.store.plateforme.application.dto.PlateformePeriodFilter;
import org.store.plateforme.application.dto.PlateformePeriodReportResponse;
import org.store.plateforme.application.service.IDepensePlateformeService;
import org.store.plateforme.application.service.IPlateformeReportingService;

import java.math.BigDecimal;

/**
 * Computes the platform P&L for a period: revenu (Revenu table, scoped by country + entreprise),
 * dépenses plateforme (scoped by country only), bénéfice = revenu − dépenses, mirroring whatever
 * country scoping the two operands share.
 */
@Service
@Transactional(readOnly = true)
public class PlateformeReportingServiceImpl implements IPlateformeReportingService {

    private final IRevenuService revenuService;
    private final IDepensePlateformeService depensePlateformeService;

    public PlateformeReportingServiceImpl(IRevenuService revenuService, IDepensePlateformeService depensePlateformeService) {
        this.revenuService = revenuService;
        this.depensePlateformeService = depensePlateformeService;
    }

    @Override
    public PlateformePeriodReportResponse getPeriodReport(PlateformePeriodFilter filter) {
        RevenuPeriodFilter revenuFilter = new RevenuPeriodFilter(filter.startDate(), filter.endDate(), filter.countryId(), filter.entrepriseId());
        BigDecimal revenu = revenuService.getTotalForPeriod(revenuFilter);
        BigDecimal depensesPlateforme = depensePlateformeService.computeTotal(filter.startDate(), filter.endDate(), filter.countryId());
        return new PlateformePeriodReportResponse(revenu, depensesPlateforme, revenu.subtract(depensesPlateforme));
    }
}
