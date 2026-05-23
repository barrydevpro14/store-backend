package org.store.abonnement.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;
import org.store.abonnement.application.dto.CouponFilter;
import org.store.abonnement.application.dto.CouponRequest;
import org.store.abonnement.application.dto.CouponResponse;
import org.store.abonnement.application.service.ICouponService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(CouponController.BASE_PATH)
public class CouponController {

    public static final String BASE_PATH = "/api/v1/coupons";

    private final ICouponService couponService;

    public CouponController(ICouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COUPON_CREATE')")
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponRequest couponRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.create(couponRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('COUPON_READ')")
    public ResponseEntity<Page<CouponResponse>> list(@RequestParam(required = false) String code,
                                                     @RequestParam(required = false) Boolean actif,
                                                     @RequestParam(required = false) UUID planId,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdStartDate,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdEndDate,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(couponService.findAll(
                new CouponFilter(code, actif, planId, createdStartDate, createdEndDate, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COUPON_READ')")
    public ResponseEntity<CouponResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(couponService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COUPON_UPDATE')")
    public ResponseEntity<CouponResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody CouponRequest couponRequest) {
        return ResponseEntity.ok(couponService.update(id, couponRequest));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('COUPON_UPDATE')")
    public ResponseEntity<CouponResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(couponService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('COUPON_UPDATE')")
    public ResponseEntity<CouponResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(couponService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COUPON_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
