package org.store.users.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.users.application.dto.EmployeRequest;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.application.service.IEmployeService;

@RestController
@RequestMapping(EmployeController.BASE_PATH)
public class EmployeController {

    public static final String BASE_PATH = "/api/v1/employees";

    private final IEmployeService employeService;

    public EmployeController(IEmployeService employeService) {
        this.employeService = employeService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYE_CREATE')")
    public ResponseEntity<EmployeResponse> create(@Valid @RequestBody EmployeRequest employeRequest) {
        EmployeResponse response = employeService.create(employeRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
