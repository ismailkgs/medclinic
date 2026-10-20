package kg.medical.platform.service;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import kg.medical.platform.dto.*;
import kg.medical.platform.entity.*;
import kg.medical.platform.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
import kg.medical.platform.repository.DoctorRepository;

@Service

public class AppointmentService {
    private final Clock clock;
    private final AppointmentRepository appointments;
    private final DoctorSlotRepository slots;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    public AppointmentService(AppointmentRepository appointments, DoctorSlotRepository slots, PatientRepository patients, DoctorRepository doctors, Clock clock) {
        this.clock = clock;
        this.appointments = appointments;
        this.slots = slots;
        this.patients = patients;
        this.doctors = doctors;
    }

    @Transactional

    public AppointmentResponse book(Long patientId, CreateAppointmentRequest request) {
        DoctorSlot slot = slots.findForUpdate(request.slotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Слот не найден"));
        Patient patient = patients.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пациент не найден"));
        if (slot.isDeleted() || !slot.getStartsAt().isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Слот недоступен для бронирования");
        }
        if (appointments.existsBySlotIdAndStatusIn(slot.getId(), List.of(AppointmentStatus.CREATED, AppointmentStatus.COMPLETED))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Слот уже занят");
        }
        Appointment saved = appointments.saveAndFlush(new Appointment(patient, slot));
        return toResponse(saved);
    }

    @Transactional

    public AppointmentResponse cancel(Long patientId, Long appointmentId) {
        Long slotId = appointments.findSlotIdById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
        slots.findForUpdate(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Слот не найден"));
        Appointment appointment = appointments.findForUpdate(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись пациента не найдена");
        }
        if (appointment.getStatus() != AppointmentStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Запись уже отменена или завершена");
        }
        if (!appointment.getStartsAt().isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Время посещения уже наступило");
        }
        appointment.cancel();
        appointments.flush();
        return toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientHistory(Long patientId, AppointmentStatus status, Long specialtyId, String doctorName) {
        if (!patients.existsById(patientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пациент не найден");
        }
        return appointments.findPatientHistory(patientId, status, specialtyId, namePattern(doctorName)).stream()
                .map(this::toResponse)
                .toList();
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        Patient patient = appointment.getPatient();
        Doctor doctor = appointment.getSlot().getDoctor();
        PatientResponse patientResponse = new PatientResponse(patient.getId(), patient.getLastName(), patient.getFirstName(), patient.getPatronymic(), patient.getPhone());
        DoctorSpecialtyResponse specialtyResponse = new DoctorSpecialtyResponse(doctor.getSpecialty().getId(), doctor.getSpecialty().getName());
        DoctorResponse doctorResponse = new DoctorResponse(doctor.getId(), doctor.getLastName(), doctor.getFirstName(), doctor.getPatronymic(), specialtyResponse);
        return new AppointmentResponse(
                appointment.getId(),
                patientResponse,
                appointment.getSlot().getId(),
                doctorResponse,
                appointment.getStatus(),
                appointment.getStartsAt(),
                appointment.getEndsAt(),
                appointment.getCreatedAt()
        );

    }
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getDoctorHistory(Long doctorId, AppointmentStatus status, String patientName) {
        if (!doctors.existsById(doctorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Врач не найден");
        }
        return appointments.findDoctorHistory(doctorId, status, namePattern(patientName)).stream()
                .map(this::toResponse)
                .toList();
    }
    @Transactional
    public AppointmentResponse changeDoctorStatus(Long doctorId, Long appointmentId, AppointmentStatus target) {
        if (target != AppointmentStatus.COMPLETED && target != AppointmentStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Разрешены статусы COMPLETED и CANCELED");
        }
        Long slotId = appointments.findSlotIdById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
        slots.findForUpdate(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Слот не найден"));
        Appointment appointment = appointments.findForUpdate(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
        if (!appointment.getSlot().getDoctor().getId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись врача не найдена");
        }
        if (appointment.getStatus() != AppointmentStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Запись уже отменена или завершена");
        }
        if (target == AppointmentStatus.COMPLETED && appointment.getStartsAt().isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Будущее посещение нельзя завершить");
        }
        if (target == AppointmentStatus.COMPLETED) {
            appointment.complete();
        } else {
            appointment.cancel();
        }
        appointments.flush();
        return toResponse(appointment);
    }
    private String namePattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String escaped = value.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }
}
