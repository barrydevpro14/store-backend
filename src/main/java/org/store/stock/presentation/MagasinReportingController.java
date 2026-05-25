package org.store.stock.presentation;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.service.CommandeAchatDomainService;
import org.store.achat.domain.enums.StatutFacture;
import org.store.stock.application.dto.MagasinOverviewStatsResponse;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.application.service.IStockService;
import org.store.stock.domain.service.StockDomainService;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.CaisseResumeResponse;
import org.store.vente.application.service.ICaisseService;
import org.store.vente.domain.service.FactureClientDomainService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(MagasinReportingController.BASE_PATH)
public class MagasinReportingController {

    public static final String BASE_PATH = "/api/v1/reporting/magasin-overview";

    private final ICaisseService caisseService;
    private final IStockService stockService;
    private final StockDomainService stockDomainService;
    private final CommandeAchatDomainService commandeAchatDomainService;
    private final FactureClientDomainService factureClientDomainService;

    public MagasinReportingController(ICaisseService caisseService,
                                      IStockService stockService,
                                      StockDomainService stockDomainService,
                                      CommandeAchatDomainService commandeAchatDomainService,
                                      FactureClientDomainService factureClientDomainService) {
        this.caisseService = caisseService;
        this.stockService = stockService;
        this.stockDomainService = stockDomainService;
        this.commandeAchatDomainService = commandeAchatDomainService;
        this.factureClientDomainService = factureClientDomainService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<MagasinOverviewStatsResponse> overview(
            @RequestParam UUID magasinId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        CaisseResumeResponse resume = caisseService.getResume(new CaisseResumeFilter(magasinId, from.toString(), to.toString()));
        StockValuationResponse valuation = stockService.computeValuation(magasinId);

        long achatsEnAttente  = commandeAchatDomainService.countByMagasinIdAndStatut(magasinId, CommandeAchatStatut.DRAFT);
        long facturesImpayees = factureClientDomainService.countByMagasinIdAndStatut(magasinId, StatutFacture.NON_PAYEE);
        long produitsBasSeuil = stockDomainService.countBelowThreshold(magasinId);

        long nombreCommandes = resume.nombreCommandes();
        BigDecimal totalCommandes = resume.totalCommandes();
        BigDecimal totalPaiements = resume.totalPaiements();
        BigDecimal ticketMoyen = nombreCommandes > 0
                ? totalCommandes.divide(BigDecimal.valueOf(nombreCommandes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ResponseEntity.ok(new MagasinOverviewStatsResponse(
                nombreCommandes,
                totalCommandes,
                totalPaiements,
                ticketMoyen,
                valuation.valeurTotale(),
                produitsBasSeuil,
                achatsEnAttente,
                facturesImpayees
        ));
    }
}
