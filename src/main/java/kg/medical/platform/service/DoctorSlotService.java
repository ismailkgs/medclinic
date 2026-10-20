package kg.medical.platform.service;
import java.time.Instant;
import java.time.Clock;
import java.util.List;
import kg.medical.platform.*;
import kg.medical.platform.dto.SlotRequest;
import kg.medical.platform.dto.SlotResponse;
import kg.medical.platform.entity.AppointmentStatus;
import kg.medical.platform.entity.Doctor;
import kg.medical.platform.entity.DoctorSlot;
import kg.medical.platform.repository.DoctorRepository;
import kg.medical.platform.repository.DoctorSlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import kg.medical.platform.repository.AppointmentRepository;

@Service

public class DoctorSlotService {
    private final Clock clock;
    private final DoctorRepository doctors;
    private final DoctorSlotRepository slots;
    private final AppointmentRepository appointments;
    public DoctorSlotService(DoctorRepository doctors, DoctorSlotRepository slots, AppointmentRepository appointments, Clock clock) {
        this.clock = clock;
        this.doctors = doctors;
        this.slots = slots;
        this.appointments = appointments;
    }
    @Transactional
    public SlotResponse create(Long doctorId, SlotRequest request) {
        Doctor doctor = doctors.findForUpdate(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Врач не найден"));
        validateInterval(request.startsAt(), request.endsAt());
        if (slots.countOverlapping(doctorId, request.startsAt(), request.endsAt()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Интервал пересекается с расписанием врача");
        }
        DoctorSlot saved = slots.save(new DoctorSlot(doctor, request.startsAt(), request.endsAt()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> getFree(Long doctorId) {
        if (!doctors.existsById(doctorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Врач не найден");
        }
        return slots.findFree(doctorId, clock.instant(), List.of(AppointmentStatus.CREATED, AppointmentStatus.COMPLETED)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SlotResponse update(Long doctorId, Long slotId, SlotRequest request) {
        DoctorSlot slot = lockEditableSlot(doctorId, slotId);
        validateInterval(request.startsAt(), request.endsAt());
        if (slots.countOtherOverlapping(doctorId, slotId, request.startsAt(), request.endsAt()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Интервал пересекается с расписанием врача");
        }
        slot.reschedule(request.startsAt(), request.endsAt());
        return toResponse(slot);
    }
    @Transactional
    public void delete(Long doctorId, Long slotId) {
        DoctorSlot slot = lockEditableSlot(doctorId, slotId);
        slot.hide();
    }
    private DoctorSlot lockEditableSlot(Long doctorId, Long slotId) {
        doctors.findForUpdate(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Врач не найден"));
        DoctorSlot slot = slots.findForUpdate(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Слот не найден"));
        if (slot.isDeleted() || !slot.getDoctor().getId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Слот врача не найден");
        }
        if (!slot.getStartsAt().isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Время слота уже наступило");
        }
        if (appointments.existsBySlotIdAndStatusIn(slotId, List.of(AppointmentStatus.CREATED, AppointmentStatus.COMPLETED))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Изменять и удалять можно только свободный слот");
        }
        return slot;
    }

    private void validateInterval(Instant startsAt, Instant endsAt) {
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Окончание должно быть позже начала");
        }
        if (!startsAt.isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Начало приёма должно быть в будущем");
        }
    }

    private SlotResponse toResponse(DoctorSlot slot) {
        return new SlotResponse(slot.getId(), slot.getDoctor().getId(), slot.getStartsAt(), slot.getEndsAt());
    }
}
