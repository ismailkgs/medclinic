package kg.medical.platform.controller;
import java.util.List;
import kg.medical.platform.dto.DoctorReportResponse;
import kg.medical.platform.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping(value="/api/doctors/{doctorId}/reports",produces="application/json;charset=UTF-8")
@PreAuthorize("@accessPolicy.doctor(#p0)")
public class DoctorReportController {
    private final ReportService reports;
    public DoctorReportController(ReportService reports) { this.reports=reports; }
    @GetMapping
    public List<DoctorReportResponse> list(@PathVariable("doctorId") Long doctorId) { return reports.list(doctorId); }
    @GetMapping("/{year}")
    public DoctorReportResponse get(@PathVariable("doctorId") Long doctorId,@PathVariable("year") int year) { return reports.get(doctorId,year); }
    @PostMapping("/{year}")
    public DoctorReportResponse request(@PathVariable("doctorId") Long doctorId,@PathVariable("year") int year) { return reports.request(doctorId,year); }
}
