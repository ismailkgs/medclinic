package kg.medical.platform.controller;

import java.util.List;
import jakarta.validation.Valid;
import kg.medical.platform.dto.AppointmentResponse;
import kg.medical.platform.dto.ChangeAppointmentStatusRequest;
import kg.medical.platform.entity.AppointmentStatus;
import kg.medical.platform.service.AppointmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/doctors/{doctorId}/appointments", produces = "application/json;charset=UTF-8")

@PreAuthorize("@accessPolicy.doctor(#p0)")

public class DoctorAppointmentController {
    private final AppointmentService service;
    public DoctorAppointmentController(AppointmentService service) {
        this.service = service;
    }
    @GetMapping
    public List<AppointmentResponse> getHistory(
          @PathVariable("doctorId") Long doctorId,
          @RequestParam(value = "status", required = false) AppointmentStatus status,
          @RequestParam(value = "patientName", required = false) String patientName
    ) {
        return service.getDoctorHistory(doctorId, status, patientName);
    }
    @PatchMapping("/{appointmentId}/status")
    public AppointmentResponse changeStatus(
           @PathVariable("doctorId") Long doctorId,
           @PathVariable("appointmentId") Long appointmentId,
           @Valid @RequestBody ChangeAppointmentStatusRequest request
    ) {
        return service.changeDoctorStatus(doctorId, appointmentId, request.status());
    }
}
