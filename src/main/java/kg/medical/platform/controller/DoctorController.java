package kg.medical.platform.controller;

import java.util.List;
import kg.medical.platform.dto.DoctorResponse;
import kg.medical.platform.service.DoctorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

@RequestMapping("/api/doctors")

public class DoctorController {
    private final DoctorService service;
    public DoctorController(DoctorService service) {
        this.service = service;
    }

    @GetMapping
    public List<DoctorResponse> getAll() {
        return service.getAll();
    }
}
