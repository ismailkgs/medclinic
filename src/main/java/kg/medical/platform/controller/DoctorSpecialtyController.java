package kg.medical.platform.controller;

import java.util.List;
import kg.medical.platform.dto.DoctorSpecialtyResponse;
import kg.medical.platform.service.DoctorSpecialtyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

@RequestMapping("/api/specialties")

public class DoctorSpecialtyController {
    private final DoctorSpecialtyService service;
    public DoctorSpecialtyController(DoctorSpecialtyService service) {
        this.service = service;
    }

    @GetMapping
    public List<DoctorSpecialtyResponse> getAll() {
        return service.getAll();
    }
}
