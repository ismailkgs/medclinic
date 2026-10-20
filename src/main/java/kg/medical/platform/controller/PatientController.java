package kg.medical.platform.controller;

import jakarta.validation.Valid;

import kg.medical.platform.dto.CreatePatientRequest;
import kg.medical.platform.dto.PatientResponse;
import kg.medical.platform.service.PatientService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")

@PreAuthorize("hasRole('ADMIN')")

public class PatientController {
    private final PatientService service;
    public PatientController(PatientService service) {
        this.service = service;
    }

    @PostMapping(produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse create(@Valid @RequestBody CreatePatientRequest request) {
        return service.create(request);
    }
}
