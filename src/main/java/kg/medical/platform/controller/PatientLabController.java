package kg.medical.platform.controller;
import java.util.List;
import jakarta.validation.Valid;
import kg.medical.platform.dto.LabBookingRequest;
import kg.medical.platform.dto.LabResponse;
import kg.medical.platform.service.LabService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping(value="/api/patients/{patientId}/lab-requests",produces="application/json;charset=UTF-8")
@PreAuthorize("@accessPolicy.patient(#p0)")
public class PatientLabController {
    private final LabService service;
    public PatientLabController(LabService service) { this.service=service; }
    @GetMapping
    public List<LabResponse> list(@PathVariable("patientId") Long patientId) { return service.patientRequests(patientId); }
    @PostMapping("/{requestId}/booking")
    public LabResponse book(@PathVariable("patientId") Long patientId,@PathVariable("requestId") Long requestId,@Valid @RequestBody LabBookingRequest request) { return service.book(patientId,requestId,request.scheduledAt()); }
    @PostMapping("/{requestId}/cancel")
    public LabResponse cancel(@PathVariable("patientId") Long patientId,@PathVariable("requestId") Long requestId) { return service.cancel(patientId,requestId); }
}
