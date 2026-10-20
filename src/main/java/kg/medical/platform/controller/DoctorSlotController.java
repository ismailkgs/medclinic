package kg.medical.platform.controller;

import java.util.List;
import jakarta.validation.Valid;
import kg.medical.platform.dto.SlotRequest;
import kg.medical.platform.dto.SlotResponse;
import kg.medical.platform.service.DoctorSlotService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.*;

@RestController

@RequestMapping("/api/doctors/{doctorId}/slots")

@PreAuthorize("@accessPolicy.doctor(#p0)")

public class DoctorSlotController {
    private final DoctorSlotService service;
    public DoctorSlotController(DoctorSlotService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse create(@PathVariable("doctorId") Long doctorId, @Valid @RequestBody SlotRequest request) {
        return service.create(doctorId, request);
    }

    @GetMapping
    public List<SlotResponse> getFree(@PathVariable("doctorId") Long doctorId) {
        return service.getFree(doctorId);
    }
    @PutMapping("/{slotId}")
    public SlotResponse update(@PathVariable("doctorId") Long doctorId, @PathVariable("slotId") Long slotId, @Valid @RequestBody SlotRequest request) {
        return service.update(doctorId, slotId, request);
    }
    @DeleteMapping("/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("doctorId") Long doctorId, @PathVariable("slotId") Long slotId) {
        service.delete(doctorId, slotId);
    }
}
