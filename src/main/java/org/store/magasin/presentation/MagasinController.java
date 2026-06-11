package org.store.magasin.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ImageDownloadResponse;
import org.store.magasin.application.dto.MagasinFilter;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.application.dto.MagasinStatsResponse;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.service.MagasinDomainService;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.application.service.IStockService;
import org.store.users.domain.service.EmployeDomainService;
import org.store.vente.domain.service.ClientDomainService;
import org.store.vente.domain.service.FactureClientDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping(MagasinController.BASE_PATH)
public class MagasinController {

    public static final String BASE_PATH = "/api/v1/magasins";

    private final IMagasinService magasinService;
    private final MagasinDomainService magasinDomainService;
    private final EmployeDomainService employeDomainService;
    private final ClientDomainService clientDomainService;
    private final IStockService stockService;
    private final FactureClientDomainService factureClientDomainService;

    public MagasinController(IMagasinService magasinService,
                             MagasinDomainService magasinDomainService,
                             EmployeDomainService employeDomainService,
                             ClientDomainService clientDomainService,
                             IStockService stockService,
                             FactureClientDomainService factureClientDomainService) {
        this.magasinService = magasinService;
        this.magasinDomainService = magasinDomainService;
        this.employeDomainService = employeDomainService;
        this.clientDomainService = clientDomainService;
        this.stockService = stockService;
        this.factureClientDomainService = factureClientDomainService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STORE_CREATE')")
    public ResponseEntity<MagasinResponse> create(@Valid @RequestBody MagasinRequest magasinRequest) {
        MagasinResponse response = magasinService.create(magasinRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STORE_READ')")
    public ResponseEntity<Page<MagasinResponse>> list(@RequestParam(required = false) String nom,
                                                      @RequestParam(required = false) Boolean actif,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdStartDate,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdEndDate,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(magasinService.findAllByCurrentEntreprise(
                new MagasinFilter(nom, actif, createdStartDate, createdEndDate, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STORE_READ_ONE')")
    public ResponseEntity<MagasinResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(magasinService.findResponseById(id));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAuthority('STORE_READ_ONE')")
    public ResponseEntity<MagasinStatsResponse> stats(@PathVariable UUID id) {
        var magasin = magasinService.ensureBelongsToCurrentEntreprise(magasinDomainService.findById(id));
        UUID entrepriseId = magasin.getEntreprise().getId();

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth   = currentMonth.atEndOfMonth().plusDays(1).atStartOfDay();

        long nombreEmployes          = employeDomainService.countByMagasinId(id);
        long nombreClients           = clientDomainService.countByEntrepriseId(entrepriseId);
        StockValuationResponse stock = stockService.computeValuation(id);
        BigDecimal revenuMois        = factureClientDomainService
                .sumMontantCommandesForCaisse(
                        new org.store.vente.application.dto.CaisseResumeFilter(
                                id,
                                startOfMonth.toLocalDate().toString(),
                                endOfMonth.minusDays(1).toLocalDate().toString()),
                        entrepriseId);

        return ResponseEntity.ok(new MagasinStatsResponse(
                nombreEmployes,
                nombreClients,
                stock.nombreLignes(),
                stock.valeurTotale(),
                revenuMois != null ? revenuMois : BigDecimal.ZERO
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('STORE_UPDATE')")
    public ResponseEntity<MagasinResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody MagasinRequest magasinRequest) {
        return ResponseEntity.ok(magasinService.update(id, magasinRequest));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('STORE_UPDATE')")
    public ResponseEntity<MagasinResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(magasinService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('STORE_UPDATE')")
    public ResponseEntity<MagasinResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(magasinService.deactivate(id));
    }

    @PutMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('STORE_UPDATE')")
    public ResponseEntity<MagasinResponse> uploadLogo(@PathVariable UUID id,
                                                     @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(magasinService.uploadLogo(id, file));
    }

    @GetMapping("/{id}/logo")
    @PreAuthorize("hasAuthority('STORE_READ_ONE')")
    public ResponseEntity<byte[]> getLogo(@PathVariable UUID id) {
        ImageDownloadResponse download = magasinService.getLogo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }

    @DeleteMapping("/{id}/logo")
    @PreAuthorize("hasAuthority('STORE_UPDATE')")
    public ResponseEntity<Void> deleteLogo(@PathVariable UUID id) {
        magasinService.deleteLogo(id);
        return ResponseEntity.noContent().build();
    }
}
