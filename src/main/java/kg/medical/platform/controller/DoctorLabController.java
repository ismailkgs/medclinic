package kg.medical.platform.controller;
import java.util.List;
import jakarta.validation.Valid;
import kg.medical.platform.dto.CreateLabRequest;
import kg.medical.platform.dto.LabResponse;
import kg.medical.platform.service.LabService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping(value="/api/doctors/{doctorId}/lab-requests",produces="application/json;charset=UTF-8")
@PreAuthorize("@accessPolicy.doctor(#p0)")
public class DoctorLabController {
    private final LabService service;
    public DoctorLabController(LabService service) { this.service=service; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabResponse create(@PathVariable("doctorId") Long doctorId,@Valid @RequestBody CreateLabRequest request) { return service.create(doctorId,request); }
    @GetMapping
    public List<LabResponse> list(@PathVariable("doctorId") Long doctorId) { return service.doctorRequests(doctorId); }
}
