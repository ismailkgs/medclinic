package kg.medical.platform.controller;

import java.util.List;

import jakarta.validation.Valid;

import kg.medical.platform.dto.AppointmentResponse;
import kg.medical.platform.dto.CreateAppointmentRequest;
import kg.medical.platform.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import kg.medical.platform.entity.AppointmentStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController

@RequestMapping(value = "/api/patients/{patientId}/appointments", produces = "application/json;charset=UTF-8")

@PreAuthorize("@accessPolicy.patient(#p0)")

public class AppointmentController {
    private final AppointmentService service;
    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping

    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@PathVariable("patientId") Long patientId, @Valid @RequestBody CreateAppointmentRequest request) {
        return service.book(patientId, request);
    }

    @PostMapping("/{appointmentId}/cancel")
    public AppointmentResponse cancel(@PathVariable("patientId") Long patientId, @PathVariable("appointmentId") Long appointmentId) {
        return service.cancel(patientId, appointmentId);
    }

    @GetMapping
    public List<AppointmentResponse> getHistory(
       @PathVariable("patientId") Long patientId,
       @RequestParam(value = "status", required = false) AppointmentStatus status,
       @RequestParam(value = "specialtyId", required = false) Long specialtyId,
       @RequestParam(value = "doctorName", required = false) String doctorName
    ) {
        return service.getPatientHistory(patientId, status, specialtyId, doctorName);
    }

}
